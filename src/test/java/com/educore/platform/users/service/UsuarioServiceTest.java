package com.educore.platform.users.service;

import com.educore.platform.users.model.Role;
import com.educore.platform.users.model.Usuario;
import com.educore.platform.users.dto.UsuarioRegistroDTO;
import com.educore.platform.users.exception.EmailAlreadyExistsException;
import com.educore.platform.users.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioServiceImpl(usuarioRepository, passwordEncoder);
    }

    @Test
    void registrar_ShouldSaveUsuarioWithVisitorRoleAndHashedPassword_WhenDataIsValid() {
        // Arrange
        UsuarioRegistroDTO dto = UsuarioRegistroDTO.builder()
                .nombre("Juan Perez")
                .email("juan@test.com")
                .password("secreto123")
                .build();

        Usuario usuarioEsperado = Usuario.builder()
                .id(1L)
                .nombre(dto.getNombre())
                .email(dto.getEmail())
                .password("hashed_password")
                .role(Role.VISITOR)
                .activo(true)
                .build();

        when(usuarioRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed_password");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioEsperado);

        // Act
        Usuario resultado = usuarioService.registrar(dto);

        // Assert
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Juan Perez", resultado.getNombre());
        assertEquals("juan@test.com", resultado.getEmail());
        assertEquals("hashed_password", resultado.getPassword());
        assertEquals(Role.VISITOR, resultado.getRole());
        assertTrue(resultado.isActivo());

        verify(usuarioRepository, times(1)).findByEmail(dto.getEmail());
        verify(passwordEncoder, times(1)).encode("secreto123");
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void registrar_ShouldThrowEmailAlreadyExistsException_WhenEmailIsAlreadyRegistered() {
        // Arrange
        UsuarioRegistroDTO dto = UsuarioRegistroDTO.builder()
                .nombre("Juan Perez")
                .email("juan@test.com")
                .password("secreto123")
                .build();

        when(usuarioRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(new Usuario()));

        // Act & Assert
        EmailAlreadyExistsException exception = assertThrows(EmailAlreadyExistsException.class, () -> {
            usuarioService.registrar(dto);
        });

        assertEquals("El correo electrónico ya está registrado: juan@test.com", exception.getMessage());
        verify(usuarioRepository, times(1)).findByEmail(dto.getEmail());
        verify(passwordEncoder, never()).encode(any());
        verify(usuarioRepository, never()).save(any());
    }
}
