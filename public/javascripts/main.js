// ==============================================================================
// Donnees
// ==============================================================================

const STORAGE_KEY = "plantshop_play_cart";
const COOKIE_NAME = "ps_play_token";
const DEBOUNCE_DELAY = 300;

// ==============================================================================
// Fonctions utilitaires
// ==============================================================================

// ------------------------------------------------------------------------------
// LocalStorage
// ------------------------------------------------------------------------------

/**
 *	Charge le panier depuis localStorage.
 *
 *	@return Objet panier indexe par id
 */
function loadCart() {
	try {
		return JSON.parse(localStorage.getItem(STORAGE_KEY) || "{}");
	} catch (parseError) {
		return {};
	}
}

/**
 *	Sauvegarde le panier et emet les evenements.
 *
 *	@param cart Objet panier a persister
 */
function saveCart(cart) {
	localStorage.setItem(STORAGE_KEY, JSON.stringify(cart));
	window.dispatchEvent(new Event("cart-updated"));
	window.dispatchEvent(new Event("storage"));
}

/**
 *	Calcule le nombre total d'articles dans le panier.
 *
 *	@return Nombre total d'articles
 */
function computeCartCount() {
	const cart = loadCart();
	return Object.values(cart).reduce(function(total, item) {
		return total + (item.quantity || 0);
	}, 0);
}

// ------------------------------------------------------------------------------
// Alerte stock
// ------------------------------------------------------------------------------

/**
 *	Affiche une alerte temporaire pour stock insuffisant.
 *
 *	@param plantName Nom de la plante
 *	@param stock Stock disponible
 */
function showStockAlert(plantName, stock) {
	const alertDiv = document.createElement("div");
	alertDiv.className = "alert alert-warning fade position-absolute top-0 start-50 translate-middle-x mt-3 shadow";
	alertDiv.setAttribute("role", "alert");
	alertDiv.style.zIndex = "1055";
	alertDiv.style.maxWidth = "600px";
	alertDiv.style.pointerEvents = "none";
	alertDiv.textContent = "Stock insuffisant pour " + plantName + " (reste " + stock + ")";
	document.body.appendChild(alertDiv);
	setTimeout(function() { alertDiv.classList.add("show"); }, 10);
	setTimeout(function() {
		alertDiv.classList.remove("show");
		setTimeout(function() { alertDiv.remove(); }, 300);
	}, 3000);
}

// ==============================================================================
// Fonctions principales
// ==============================================================================

// ------------------------------------------------------------------------------
// Panier - Actions
// ------------------------------------------------------------------------------

/**
 *	Ajoute une plante au panier ou incremente la quantite.
 *
 *	@param plantId Identifiant de la plante
 *	@param plantName Nom de la plante
 *	@param plantPrice Prix unitaire
 *	@param plantStock Stock disponible
 */
function addToCart(plantId, plantName, plantPrice, plantStock) {
	const cart = loadCart();
	if (!cart[plantId]) {
		cart[plantId] = { id: plantId, name: plantName, price: plantPrice, quantity: 0, stock: plantStock };
	}
	if (cart[plantId].quantity >= plantStock) {
		showStockAlert(plantName, plantStock);
		setTimeout(function() {
			cart[plantId].quantity = plantStock;
			saveCart(cart);
		}, DEBOUNCE_DELAY);
	} else {
		cart[plantId].quantity++;
		saveCart(cart);
	}
	updateNavbarCount();
}

/**
 *	Met a jour la quantite d'un article avec debounce.
 *
 *	@param plantId Identifiant de la plante
 *	@param inputElement Element input HTML
 */
function delayedUpdateCart(plantId, inputElement) {
	clearTimeout(inputElement._debounceTimer);
	inputElement._debounceTimer = setTimeout(function() {
		updateCartItem(plantId, inputElement.value);
	}, DEBOUNCE_DELAY);
}

/**
 *	Met a jour la quantite d'un article dans le panier.
 *
 *	@param plantId Identifiant de la plante
 *	@param value Nouvelle quantite saisie
 */
function updateCartItem(plantId, value) {
	const quantity = parseInt(value, 10);
	if (isNaN(quantity)) return;
	const cart = loadCart();
	if (!cart[plantId]) return;
	const stock = cart[plantId].stock || 1;
	const corrected = Math.min(Math.max(quantity, 1), stock);
	cart[plantId].quantity = corrected;
	saveCart(cart);
	renderCart();
}

/**
 *	Retire un article du panier.
 *
 *	@param plantId Identifiant de la plante
 */
function removeFromCart(plantId) {
	const cart = loadCart();
	delete cart[plantId];
	saveCart(cart);
	renderCart();
}

/**
 *	Vide entierement le panier.
 */
function clearCart() {
	localStorage.removeItem(STORAGE_KEY);
	window.dispatchEvent(new Event("cart-updated"));
	renderCart();
}

// ------------------------------------------------------------------------------
// Navbar
// ------------------------------------------------------------------------------

/**
 *	Met a jour le compteur du panier dans la navbar.
 */
function updateNavbarCount() {
	const link = document.getElementById("cart-link");
	if (!link) return;
	const count = computeCartCount();
	link.textContent = "Mon Panier" + (count > 0 ? " (" + count + ")" : "");
}

// ------------------------------------------------------------------------------
// Rendu panier
// ------------------------------------------------------------------------------

/**
 *	Affiche le contenu du panier dans #cart-container.
 */
function renderCart() {
	const container = document.getElementById("cart-container");
	if (!container) return;
	const cart = loadCart();
	updateNavbarCount();
	container.textContent = "";
	if (Object.keys(cart).length === 0) {
		const emptyAlert = document.createElement("p");
		emptyAlert.className = "alert alert-info";
		emptyAlert.textContent = "Votre panier est vide.";
		container.appendChild(emptyAlert);
		return;
	}
	renderCartTable(container, cart);
	renderCartFooter(container, cart);
}

/**
 *	Construit le tableau du panier.
 *
 *	@param container Element DOM conteneur
 *	@param cart Objet panier
 */
function renderCartTable(container, cart) {
	const table = document.createElement("table");
	table.className = "table";
	const thead = document.createElement("thead");
	thead.className = "table-light";
	thead.innerHTML = "<tr><th>Plante</th><th>Quantité</th><th>Action</th></tr>";
	table.appendChild(thead);
	const tbody = document.createElement("tbody");
	for (const plantId in cart) {
		const item = cart[plantId];
		const row = document.createElement("tr");
		row.appendChild(createPlantCell(item));
		row.appendChild(createQuantityCell(item));
		row.appendChild(createActionCell(item));
		tbody.appendChild(row);
	}
	table.appendChild(tbody);
	container.appendChild(table);
}

/**
 *	Cree la cellule nom de la plante.
 *
 *	@param item Item du panier
 *	@return Element TD
 */
function createPlantCell(item) {
	const cell = document.createElement("td");
	const link = document.createElement("a");
	link.href = "/plants/" + item.id;
	link.className = "text-decoration-none";
	link.textContent = item.name;
	cell.appendChild(link);
	return cell;
}

/**
 *	Cree la cellule quantite avec input.
 *
 *	@param item Item du panier
 *	@return Element TD
 */
function createQuantityCell(item) {
	const cell = document.createElement("td");
	const input = document.createElement("input");
	input.type = "number";
	input.min = "1";
	input.max = String(item.stock);
	input.className = "form-control form-control-sm";
	input.style.maxWidth = "70px";
	input.value = String(item.quantity);
	input.oninput = function() { delayedUpdateCart(item.id, input); };
	cell.appendChild(input);
	return cell;
}

/**
 *	Cree la cellule action (bouton retirer).
 *
 *	@param item Item du panier
 *	@return Element TD
 */
function createActionCell(item) {
	const cell = document.createElement("td");
	const button = document.createElement("button");
	button.className = "btn btn-danger btn-sm";
	button.textContent = "Retirer";
	button.onclick = function() { removeFromCart(item.id); };
	cell.appendChild(button);
	return cell;
}

/**
 *	Affiche le total et les boutons sous le tableau.
 *
 *	@param container Element DOM conteneur
 *	@param cart Objet panier
 */
function renderCartFooter(container, cart) {
	let total = 0;
	for (const plantId in cart) {
		total += cart[plantId].price * cart[plantId].quantity;
	}
	const totalParagraph = document.createElement("p");
	totalParagraph.className = "text-end fw-bold";
	totalParagraph.textContent = "Total : " + total + " €";
	container.appendChild(totalParagraph);
	const footer = document.createElement("div");
	footer.className = "d-flex justify-content-between";
	const clearButton = document.createElement("button");
	clearButton.className = "btn btn-outline-secondary btn-sm";
	clearButton.textContent = "Vider le panier";
	clearButton.onclick = clearCart;
	footer.appendChild(clearButton);
	const orderLink = document.createElement("a");
	orderLink.href = "/orders/new";
	orderLink.className = "btn btn-primary";
	orderLink.textContent = "Passer la commande";
	footer.appendChild(orderLink);
	container.appendChild(footer);
}

// ==============================================================================
// Main
// ==============================================================================

/**
 *	Initialise le panier au chargement de la page.
 */
function main() {
	updateNavbarCount();
	renderCart();
	window.addEventListener("cart-updated", updateNavbarCount);
	window.addEventListener("storage", updateNavbarCount);
}

// ==============================================================================
// Lancement du programme
// ==============================================================================

document.addEventListener("DOMContentLoaded", main);