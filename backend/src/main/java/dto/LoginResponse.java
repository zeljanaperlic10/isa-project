package dto;

public class LoginResponse {
    
    private String token;
    private String type = "Bearer"; // Tip tokena (uvek Bearer za JWT)
    private UserDTO user;
    
    // Konstruktor prazan
    public LoginResponse() {}
    
    // Konstruktor sa poljima
    public LoginResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }
    
    // Getteri i Setteri
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}
