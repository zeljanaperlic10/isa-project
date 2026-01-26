package model;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Naziv taga (npr. "programiranje", "priroda", "muzika")
    @Column(unique = true, nullable = false, length = 50)
    private String name;

    // Relacija sa postovima (Many-to-Many - drugi kraj)
    @ManyToMany(mappedBy = "tags")
    private Set<Post> posts = new HashSet<>();

    // Broj postova sa ovim tagom (za statistiku/popularnost)
    @Column(nullable = false)
    private Integer postCount = 0;

    // ============================================
    // KONSTRUKTORI
    // ============================================

    public Tag() {}

    public Tag(String name) {
        this.name = name.toLowerCase().trim(); // Normalizacija (mala slova, bez razmaka)
    }

    // ============================================
    // GETTERI I SETTERI
    // ============================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.toLowerCase().trim();
    }

    public Set<Post> getPosts() {
        return posts;
    }

    public void setPosts(Set<Post> posts) {
        this.posts = posts;
    }

    public Integer getPostCount() {
        return postCount;
    }

    public void setPostCount(Integer postCount) {
        this.postCount = postCount;
    }

    // ============================================
    // EQUALS & HASHCODE (važno za Set kolekcije!)
    // ============================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return name != null && name.equals(tag.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return name;
    }
}