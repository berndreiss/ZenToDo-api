package net.berndreiss.zentodo.data;

import jakarta.persistence.*;

/**
 * TODO DESCRIBE
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column
    private String userName = null;

    @Column
    private String token;

    public User(){}
    public User(Long id, String email){
        this.id = id;
        this.email = email;
    }
    public User(Long id, String email, String username){
        this.id = id;
        this.email = email;
        this.userName = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
