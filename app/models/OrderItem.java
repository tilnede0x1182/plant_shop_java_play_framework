package models;

// ==============================================================================
// Importations
// ==============================================================================

import io.ebean.Model;
import io.ebean.Finder;
import jakarta.persistence.*;
import java.math.BigDecimal;

// ==============================================================================
// Modele OrderItem
// ==============================================================================

/**
 *	Modele representant un item dans une commande.
 *	Lie une plante a une commande avec une quantite.
 */
@Entity
@Table(name = "order_items")
public class OrderItem extends Model {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne
	@JoinColumn(name = "plant_id")
	private Plant plant;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private BigDecimal price;

	// ------------------------------------------------------------------------------
	// Finder
	// ------------------------------------------------------------------------------

	public static final Finder<Long, OrderItem> find = new Finder<>(OrderItem.class);

	// ------------------------------------------------------------------------------
	// Getters
	// ------------------------------------------------------------------------------

	public Long getId() { return id; }
	public Order getOrder() { return order; }
	public Plant getPlant() { return plant; }
	public int getQuantity() { return quantity; }
	public BigDecimal getPrice() { return price; }

	// ------------------------------------------------------------------------------
	// Setters
	// ------------------------------------------------------------------------------

	public void setOrder(Order order) { this.order = order; }
	public void setPlant(Plant plant) { this.plant = plant; }
	public void setQuantity(int quantity) { this.quantity = quantity; }
	public void setPrice(BigDecimal price) { this.price = price; }
}