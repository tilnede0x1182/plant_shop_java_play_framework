package models;

// ==============================================================================
// Importations
// ==============================================================================

import io.ebean.Model;
import io.ebean.Finder;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

// ==============================================================================
// Modele User
// ==============================================================================

/**
 *	Modele utilisateur avec authentification et role admin.
 *	Mappe la table "users" en base de donnees.
 */
@Entity
@Table(name = "users")
public class User extends Model {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String name;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "is_admin", nullable = false)
	private boolean isAdmin = false;

	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<Order> orders;

	// ------------------------------------------------------------------------------
	// Finder
	// ------------------------------------------------------------------------------

	public static final Finder<Long, User> find = new Finder<>(User.class);

	// ------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------

	/**
	 *	@return Identifiant unique
	 */
	public Long getId() { return id; }

	/**
	 *	@return Adresse email
	 */
	public String getEmail() { return email; }

	/**
	 *	@return Nom complet
	 */
	public String getName() { return name; }

	/**
	 *	@return Hash du mot de passe
	 */
	public String getPasswordHash() { return passwordHash; }

	/**
	 *	@return True si administrateur
	 */
	public boolean isAdmin() { return isAdmin; }

	/**
	 *	@return Date de creation
	 */
	public Instant getCreatedAt() { return createdAt; }

	/**
	 *	@return Liste des commandes
	 */
	public List<Order> getOrders() { return orders; }

	// ------------------------------------------------------------------------------
	// Setters
	// ------------------------------------------------------------------------------

	/**
	 *	@param email Adresse email
	 */
	public void setEmail(String email) { this.email = email; }

	/**
	 *	@param name Nom complet
	 */
	public void setName(String name) { this.name = name; }

	/**
	 *	@param passwordHash Hash bcrypt
	 */
	public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

	/**
	 *	@param isAdmin Role administrateur
	 */
	public void setIsAdmin(boolean isAdmin) { this.isAdmin = isAdmin; }
}