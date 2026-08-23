# =====================================================================
#  test-falabella.ps1
#  Prueba las credenciales de Falabella Seller Center SIN levantar el
#  backend. Replica exactamente la firma HMAC de HmacSignatureService.java
#  (mismo string-to-sign, mismo URL-encoding estilo Java URLEncoder).
#
#  Uso:
#     powershell -ExecutionPolicy Bypass -File .\test-falabella.ps1
# =====================================================================

$UserId   = "ximena.huala@usach.cl"
$ApiKey   = "d8885527dea9577e51efd73a85ba978b072d3b03"
$SellerId = "SC459DF"
$BaseUrl  = "https://sellercenter-api.falabella.com"
$Version  = "1.0"

# PowerShell 5.1 a veces negocia TLS 1.0 y Falabella lo rechaza.
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# --- URL encode identico a java.net.URLEncoder.encode(s, UTF_8) ---
function Enc([string]$s) {
    $sb = New-Object System.Text.StringBuilder
    foreach ($b in [System.Text.Encoding]::UTF8.GetBytes($s)) {
        $c = [char]$b
        if (($c -ge 'a' -and $c -le 'z') -or ($c -ge 'A' -and $c -le 'Z') -or
            ($c -ge '0' -and $c -le '9') -or $c -eq '.' -or $c -eq '-' -or
            $c -eq '*' -or $c -eq '_') {
            [void]$sb.Append($c)
        } elseif ($c -eq ' ') {
            [void]$sb.Append('+')
        } else {
            [void]$sb.AppendFormat('%{0:X2}', $b)
        }
    }
    $sb.ToString()
}

function Sign([hashtable]$p, [string]$key) {
    $sts = (($p.Keys | Sort-Object -CaseSensitive | ForEach-Object {
        (Enc $_) + "=" + (Enc $p[$_])
    }) -join "&")
    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($key)
    $bytes = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($sts))
    ($bytes | ForEach-Object { $_.ToString("x2") }) -join ''
}

function Invoke-Falabella([string]$Action, [hashtable]$Extra, [switch]$RomperFirma) {
    $p = @{
        Action    = $Action
        Format    = "JSON"
        Timestamp = (Get-Date).ToString("yyyy-MM-dd\THH:mm:sszzz")
        UserID    = $UserId
        Version   = $Version
    }
    if ($Extra) { $Extra.GetEnumerator() | ForEach-Object { $p[$_.Key] = $_.Value } }

    $sig = Sign $p $ApiKey
    if ($RomperFirma) {
        $ultimo = $sig[-1]
        $sig = $sig.Substring(0, $sig.Length - 1) + $(if ($ultimo -eq 'a') { 'b' } else { 'a' })
    }
    $p["Signature"] = $sig

    $query = (($p.Keys | Sort-Object -CaseSensitive | ForEach-Object {
        (Enc $_) + "=" + (Enc $p[$_])
    }) -join "&")
    $url = "$BaseUrl/?$query"

    try {
        $r = Invoke-WebRequest -Uri $url -Headers @{ "User-Agent" = "$SellerId/Java/21" } `
                               -UseBasicParsing -TimeoutSec 30
        return @{ Code = $r.StatusCode; Body = $r.Content }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
            return @{ Code = [int]$resp.StatusCode; Body = $reader.ReadToEnd() }
        }
        return @{ Code = "SIN_RED"; Body = $_.Exception.Message }
    }
}

function Show($titulo, $res) {
    Write-Host ""
    Write-Host "=== $titulo  ->  HTTP $($res.Code)" -ForegroundColor Cyan
    $b = $res.Body
    if ($b.Length -gt 1500) { $b = $b.Substring(0, 1500) + " ...[truncado]" }
    Write-Host $b
}

Write-Host "UserID  : $UserId"
Write-Host "SellerID: $SellerId"
Write-Host "ApiKey  : $($ApiKey.Substring(0,6))...$($ApiKey.Substring($ApiKey.Length-4))  ($($ApiKey.Length) chars)"

# 1) Control negativo: firma rota a proposito. Debe responder E007 (Invalid Signature).
Show "1. Firma ROTA a proposito (se espera E007)" (Invoke-Falabella -Action "GetMetrics" -Extra @{ StatisticsType = "alltime" } -RomperFirma)

# 2) Firma correcta, endpoint mas simple.
Show "2. GetMetrics (firma correcta)" (Invoke-Falabella -Action "GetMetrics" -Extra @{ StatisticsType = "alltime" })

# 3) El endpoint que realmente necesita el sync.
Show "3. GetOrders" (Invoke-Falabella -Action "GetOrders" -Extra @{
    CreatedAfter  = "2026-01-01T00:00:00-04:00"
    CreatedBefore = "2026-02-01T00:00:00-04:00"
    Limit         = "5"
    Offset        = "0"
})

# 4) Catalogo, para confirmar que la cuenta ve productos.
Show "4. GetProducts" (Invoke-Falabella -Action "GetProducts" -Extra @{ Limit = "2"; Offset = "0" })

Write-Host ""
Write-Host "--------------------------------------------------------------"
Write-Host "COMO LEER EL RESULTADO:"
Write-Host "  * Prueba 1 da E007 y las otras dan 200  -> credenciales OK."
Write-Host "  * Prueba 1 da E007 y 2/3/4 dan E009     -> la firma esta bien;"
Write-Host "    el problema es de PERMISOS de la cuenta (o SELLER_ID equivocado)."
Write-Host "  * TODAS dan E007                        -> la API Key no corresponde al UserID."
Write-Host "  * TODAS dan SIN_RED                     -> firewall/proxy, no es la credencial."
Write-Host "--------------------------------------------------------------"
