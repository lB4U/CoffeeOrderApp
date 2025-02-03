package App;

public class AppUser {
    int id;
    String username;
    String password;
    Role role;

    public AppUser(int id, String username, String password, Role role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
