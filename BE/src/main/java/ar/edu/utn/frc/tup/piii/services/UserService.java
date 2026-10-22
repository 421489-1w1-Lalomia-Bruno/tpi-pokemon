package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponse;
import ar.edu.utn.frc.tup.piii.dtos.auth.RegisterRequest;
import ar.edu.utn.frc.tup.piii.dtos.auth.UserProfileDTO;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(String username, String password);
    UserProfileDTO getProfile(String userId);
}
