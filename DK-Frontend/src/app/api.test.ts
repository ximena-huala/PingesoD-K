import { describe, it, expect, beforeEach, vi } from "vitest";
import { login, logout, isAuthed, getResumen } from "./api";

/**
 * Pruebas del cliente de API del frontend: manejo del token JWT,
 * autenticación, header Authorization y cierre de sesión ante un 401.
 * `fetch` se mockea; no hay backend real.
 */
describe("cliente de API (auth y token)", () => {
  beforeEach(() => {
    logout(); // resetea el token del módulo + localStorage
    vi.unstubAllGlobals();
  });

  it("login guarda el token y deja la sesión autenticada", async () => {
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ token: "jwt-123", nombre: "Kevin", email: "kevin@dk.cl" }),
    });
    vi.stubGlobal("fetch", fetchMock);

    const res = await login("kevin@dk.cl", "changeme");

    expect(res.token).toBe("jwt-123");
    expect(isAuthed()).toBe(true);
    expect(localStorage.getItem("dk_token")).toBe("jwt-123");
    expect(fetchMock).toHaveBeenCalledWith(
      expect.stringContaining("/api/auth/login"),
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("login con credenciales inválidas lanza error y no autentica", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false, status: 401 }));

    await expect(login("x@x.cl", "mala")).rejects.toThrow("Credenciales inválidas");
    expect(isAuthed()).toBe(false);
  });

  it("logout limpia el token y la sesión", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ token: "t", nombre: "n", email: "e" }),
    }));
    await login("a@a.cl", "x");
    expect(isAuthed()).toBe(true);

    logout();

    expect(isAuthed()).toBe(false);
    expect(localStorage.getItem("dk_token")).toBeNull();
  });

  it("las peticiones autenticadas adjuntan el token en Authorization", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ token: "jwt-xyz", nombre: "n", email: "e" }),
    }));
    await login("a@a.cl", "x");

    const resumenFetch = vi.fn().mockResolvedValue({ ok: true, json: async () => ({ totales: {} }) });
    vi.stubGlobal("fetch", resumenFetch);
    await getResumen();

    expect(resumenFetch).toHaveBeenCalledWith(
      expect.stringContaining("/api/rentabilidad/resumen"),
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: "Bearer jwt-xyz" }),
      }),
    );
  });

  it("una respuesta 401 cierra la sesión automáticamente", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ token: "jwt", nombre: "n", email: "e" }),
    }));
    await login("a@a.cl", "x");
    expect(isAuthed()).toBe(true);

    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: false, status: 401 }));
    await expect(getResumen()).rejects.toThrow(/Sesión expirada/);
    expect(isAuthed()).toBe(false); // logout automático
  });
});
