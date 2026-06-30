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
// Modele Order
// ==============================================================================

/**
 *	Modele representant une commande client.
 *	Mappe la table "orders" en base de donnees.
 */
@Entity
@Table(name = "orders")
public class Order extends Model {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	@Column(nullable = false)
	private BigDecimal total = BigDecimal.ZERO;

	@Column(nullable = false, length = 50)
	private String status = "pending";

	@Column(name = "created_at")
	private Instant createdAt = Instant.now();

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
	private List<OrderItem> orderItems;

	// ------------------------------------------------------------------------------
	// Finder
	// ------------------------------------------------------------------------------

	public static final Finder<Long, Order> find = new Finder<>(Order.class);

	// ------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------

	public Long getId() { return id; }
	public User getUser() { return user; }
	public BigDecimal getTotal() { return total; }
	public String getStatus() { return status; }
	public Instant getCreatedAt() { return createdAt; }
	public List<OrderItem> getOrderItems() { return orderItems; }

	// ------------------------------------------------------------------------------
	// Setters
	// ------------------------------------------------------------------------------

	public void setUser(User user) { this.user = user; }
	public void setTotal(BigDecimal total) { this.total = total; }
	public void setStatus(String status) { this.status = status; }
}