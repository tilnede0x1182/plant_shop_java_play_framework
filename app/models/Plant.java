package models;

// ==============================================================================
// Importations
// ==============================================================================

import io.ebean.Model;
import io.ebean.Finder;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// ==============================================================================
// Modele Plant
// ==============================================================================

/**
 *	Modele representant une plante en vente.
 *	Mappe la table "plants" en base de donnees.
 */
@Entity
@Table(name = "plants")
public class Plant extends Model {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	private String description;

	@Column(nullable = false)
	private BigDecimal price;

	@Column(nullable = false)
	private int stock = 0;

	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "plant")
	private List<OrderItem> orderItems;

	// ------------------------------------------------------------------------------
	// Finder
	// ------------------------------------------------------------------------------

	public static final Finder<Long, Plant> find = new Finder<>(Plant.class);

	// ------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------

	public Long getId() { return id; }
	public String getName() { return name; }
	public String getDescription() { return description; }
	public BigDecimal getPrice() { return price; }
	public int getStock() { return stock; }
	public Instant getCreatedAt() { return createdAt; }
	public List<OrderItem> getOrderItems() { return orderItems; }

	// ------------------------------------------------------------------------------
	// Setters
	// ------------------------------------------------------------------------------

	public void setName(String name) { this.name = name; }
	public void setDescription(String description) { this.description = description; }
	public void setPrice(BigDecimal price) { this.price = price; }
	public void setStock(int stock) { this.stock = stock; }
}