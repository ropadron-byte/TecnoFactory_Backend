package com.factory.tecnofactory.controller;

import com.factory.tecnofactory.dto.UsuarioResponse;
import com.factory.tecnofactory.exception.RecursoNoEncontradoException;
import com.factory.tecnofactory.exception.ReglaNegocioException;
import com.factory.tecnofactory.security.JwtService;
import com.factory.tecnofactory.security.SecurityConfig;
import com.factory.tecnofactory.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Baja de cuentas: quién puede desactivar o borrar qué. */
@WebMvcTest(UsuarioController.class)
@Import(SecurityConfig.class)
class UsuarioControllerBajaTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UsuarioService usuarioService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor cliente() {
        return user("ana@correo.cl").roles("CLIENTE");
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor vendedor() {
        return user("luis@correo.cl").roles("VENDEDOR");
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return user("admin@correo.cl").roles("ADMINISTRADOR");
    }

    @Test
    void clienteDesactivaSuCuentaConElCorreoDeLaSesion() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me/desactivar").with(cliente()))
                .andExpect(status().isNoContent());
        verify(usuarioService).desactivarMiCuenta("ana@correo.cl");
    }

    @Test
    void clienteNoPuedeBorrarSuCuenta() throws Exception {
        // DELETE /me cae en la regla de administrador: el cliente recibe 403 y nada se borra
        mockMvc.perform(delete("/api/usuarios/me").with(cliente())).andExpect(status().isForbidden());
        verify(usuarioService, never()).eliminarVendedor(any());
    }

    @Test
    void vendedorYAdministradorNoUsanLaDesactivacionDeCliente() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me/desactivar").with(vendedor())).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/usuarios/me/desactivar").with(admin())).andExpect(status().isForbidden());
        verify(usuarioService, never()).desactivarMiCuenta(any());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(patch("/api/usuarios/me/desactivar")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/usuarios/2/desactivar")).andExpect(status().isUnauthorized());
    }

    @Test
    void administradorDesactivaUnVendedor() throws Exception {
        when(usuarioService.desactivarVendedor(2L)).thenReturn(new UsuarioResponse(2L, 6000000, "K", "Luis", "Soto",
                "luis@correo.cl", LocalDate.of(1990, 5, 20), "Calle 1", "+56912345678",
                false, "VENDEDOR", "Santiago", "Metropolitana"));

        mockMvc.perform(patch("/api/usuarios/2/desactivar").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false))
                .andExpect(jsonPath("$.tipoUsuario").value("VENDEDOR"));
    }

    @Test
    void administradorBorraUnVendedor() throws Exception {
        mockMvc.perform(delete("/api/usuarios/2").with(admin())).andExpect(status().isNoContent());
        verify(usuarioService).eliminarVendedor(2L);
    }

    @Test
    void clienteYVendedorNoPuedenGestionarVendedores() throws Exception {
        mockMvc.perform(delete("/api/usuarios/2").with(cliente())).andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/usuarios/2/desactivar").with(vendedor())).andExpect(status().isForbidden());
        verify(usuarioService, never()).eliminarVendedor(any());
        verify(usuarioService, never()).desactivarVendedor(any());
    }

    @Test
    void borrarUnVendedorConBlogsDevuelve409() throws Exception {
        doThrow(new ReglaNegocioException("El vendedor tiene blogs asociados; desactiva su cuenta en lugar de borrarla"))
                .when(usuarioService).eliminarVendedor(2L);

        mockMvc.perform(delete("/api/usuarios/2").with(admin()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void borrarVendedorInexistenteDevuelve404() throws Exception {
        doThrow(new RecursoNoEncontradoException("Usuario", 99L)).when(usuarioService).eliminarVendedor(99L);

        mockMvc.perform(delete("/api/usuarios/99").with(admin())).andExpect(status().isNotFound());
    }
}
