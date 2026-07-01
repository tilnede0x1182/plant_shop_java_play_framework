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
// Capitalisation
// ------------------------------------------------------------------------------

/**
 *	Capitalise chaque mot d une chaine.
 *	Exception : "de" reste en minuscule sauf en premiere position.
 *
 *	@param str Chaine a capitaliser
 *	@return Chaine capitalisee
 */
function capitalize(str) {
	if (!str) return "";
	return str.split(" ").map(function(word, index) {
		const lower = word.toLowerCase();
		if (index > 0 && lower === "de") return lower;
		return lower.charAt(0).toUpperCase() + lower.slice(1);
	}).join(" ");
}

/**
 *	Applique la capitalisation au nom affiche dans la navbar.
 */
function capitalizeDisplayName() {
	const nameElement = document.getElementById("user-display-name");
	if (nameElement) {
		nameElement.textContent = capitalize(nameElement.textContent);
	}
}

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

// ------------------------------------------------------------------------------
// Auth - Login
// ------------------------------------------------------------------------------

/**
 *	Gere la soumission du formulaire de connexion.
 *	POST /api/auth/login avec email et password.
 */
function initLoginForm() {
	const form = document.getElementById("login-form");
	if (!form) return;
	form.addEventListener("submit", async function(submitEvent) {
		submitEvent.preventDefault();
		const email = document.getElementById("login-email").value;
		const password = document.getElementById("login-password").value;
		try {
			const response = await fetch("/api/auth/login", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ email: email, password: password }),
			});
			if (response.status === 201) {
				window.location.href = "/plants";
			} else {
				showFlashError("Identifiants invalides");
			}
		} catch (fetchError) {
			showFlashError("Erreur de connexion");
		}
	});
}

// ------------------------------------------------------------------------------
// Auth - Register
// ------------------------------------------------------------------------------

/**
 *	Gere la soumission du formulaire d inscription.
 *	POST /api/auth/register avec name, email et password.
 */
function initRegisterForm() {
	const form = document.getElementById("register-form");
	if (!form) return;
	form.addEventListener("submit", async function(submitEvent) {
		submitEvent.preventDefault();
		const name = document.getElementById("register-name").value;
		const email = document.getElementById("register-email").value;
		const password = document.getElementById("register-password").value;
		try {
			const response = await fetch("/api/auth/register", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ name: name, email: email, password: password }),
			});
			if (response.status === 201) {
				window.location.href = "/plants";
			} else {
				showFlashError("Email deja utilise");
			}
		} catch (fetchError) {
			showFlashError("Erreur lors de l inscription");
		}
	});
}

// ------------------------------------------------------------------------------
// Auth - Flash messages
// ------------------------------------------------------------------------------

/**
 *	Affiche un message d erreur dans le flash-error.
 *
 *	@param message Texte de l erreur
 */
function showFlashError(message) {
	const flashDiv = document.getElementById("flash-error");
	if (!flashDiv) return;
	flashDiv.textContent = message;
	flashDiv.classList.remove("d-none");
}

// ------------------------------------------------------------------------------
// Commande
// ------------------------------------------------------------------------------

/**
 *	Gere la confirmation de commande.
 *	POST /api/orders avec les items du panier.
 */
/**
 *	Affiche le recapitulatif de commande depuis le panier localStorage.
 */
function renderOrderReview() {
	const container = document.getElementById("order-review-container");
	if (!container) return;
	const cart = loadCart();
	const items = Object.values(cart);
	if (items.length === 0) {
		container.innerHTML = '<div class="alert alert-info">Votre panier est vide.</div>';
		const confirmBtn = document.getElementById("confirm-order-btn");
		if (confirmBtn) confirmBtn.style.display = "none";
		return;
	}
	renderOrderTable(container, items);
}

/**
 *	Construit le tableau recapitulatif de commande.
 *
 *	@param container Element DOM conteneur
 *	@param items Tableau des items du panier
 */
function renderOrderTable(container, items) {
	const table = document.createElement("table");
	table.className = "table shadow";
	table.innerHTML = '<thead class="table-light"><tr><th>Plante</th><th>Quantité</th><th>Total</th></tr></thead>';
	const tbody = document.createElement("tbody");
	let total = 0;
	for (const item of items) {
		const row = document.createElement("tr");
		row.innerHTML = '<td>' + item.name + '</td><td>' + item.quantity + '</td><td>' + (item.price * item.quantity) + ' €</td>';
		tbody.appendChild(row);
		total += item.price * item.quantity;
	}
	table.appendChild(tbody);
	container.appendChild(table);
	const totalParagraph = document.createElement("p");
	totalParagraph.className = "fw-bold text-end";
	totalParagraph.textContent = "Total : " + total + " €";
	container.appendChild(totalParagraph);
}

function initConfirmOrder() {
	const button = document.getElementById("confirm-order-btn");
	if (!button) return;
	button.addEventListener("click", async function() {
		const cart = loadCart();
		const items = Object.values(cart).map(function(item) {
			return { plant_id: item.id, quantity: item.quantity };
		});
		if (items.length === 0) return;
		try {
			const response = await fetch("/api/orders", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ items: items }),
			});
			if (response.status === 201) {
				localStorage.removeItem(STORAGE_KEY);
				window.dispatchEvent(new Event("cart-updated"));
				window.location.href = "/orders";
			} else {
				showFlashError("Erreur lors de la commande");
			}
		} catch (fetchError) {
			showFlashError("Erreur de connexion");
		}
	});
}

// ------------------------------------------------------------------------------
// Admin - CRUD Plantes
// ------------------------------------------------------------------------------

/**
 *	Gere la creation d une nouvelle plante.
 *	#plant-form -> POST /api/admin/plants
 */
function initPlantForm() {
	const form = document.getElementById("plant-form");
	if (!form) return;
	form.addEventListener("submit", async function(submitEvent) {
		submitEvent.preventDefault();
		const body = {
			name: document.getElementById("plant-name").value,
			description: document.getElementById("plant-description").value,
			price: parseFloat(document.getElementById("plant-price").value),
			stock: parseInt(document.getElementById("plant-stock").value),
		};
		try {
			const response = await fetch("/api/admin/plants", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(body),
			});
			if (response.status === 201) { window.location.href = "/admin/plants"; }
			else { showFlashError("Erreur lors de la creation"); }
		} catch (fetchError) { showFlashError("Erreur de connexion"); }
	});
}

/**
 *	Gere la modification d une plante.
 *	#plant-edit-form -> PATCH /api/admin/plants/:id
 */
function initPlantEditForm() {
	const form = document.getElementById("plant-edit-form");
	if (!form) return;
	const plantId = form.dataset.id;
	form.addEventListener("submit", async function(submitEvent) {
		submitEvent.preventDefault();
		const body = {
			name: document.getElementById("plant-name").value,
			description: document.getElementById("plant-description").value,
			price: parseFloat(document.getElementById("plant-price").value),
			stock: parseInt(document.getElementById("plant-stock").value),
		};
		try {
			const response = await fetch("/api/admin/plants/" + plantId, {
				method: "PATCH",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(body),
			});
			if (response.ok) { window.location.href = "/admin/plants"; }
			else { showFlashError("Erreur lors de la modification"); }
		} catch (fetchError) { showFlashError("Erreur de connexion"); }
	});
}

/**
 *	Supprime une plante apres confirmation.
 *	DELETE /api/admin/plants/:id
 *
 *	@param plantId Identifiant de la plante
 */
function deletePlant(plantId) {
	if (!confirm("Supprimer cette plante ?")) return;
	fetch("/api/admin/plants/" + plantId, { method: "DELETE" })
		.then(function(response) {
			if (response.ok) { window.location.href = "/admin/plants"; }
			else { showFlashError("Erreur lors de la suppression"); }
		})
		.catch(function() { showFlashError("Erreur de connexion"); });
}

// ------------------------------------------------------------------------------
// Admin - CRUD Utilisateurs
// ------------------------------------------------------------------------------

/**
 *	Gere la modification d un utilisateur.
 *	#user-edit-form -> PATCH /api/admin/users/:id
 */
function initUserEditForm() {
	const form = document.getElementById("user-edit-form");
	if (!form) return;
	const userId = form.dataset.id;
	form.addEventListener("submit", async function(submitEvent) {
		submitEvent.preventDefault();
		const body = {
			name: document.getElementById("user-name").value,
			email: document.getElementById("user-email").value,
			admin: document.getElementById("user-admin").checked,
		};
		try {
			const response = await fetch("/api/admin/users/" + userId, {
				method: "PATCH",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(body),
			});
			if (response.ok) { window.location.href = "/admin/users"; }
			else { showFlashError("Erreur lors de la modification"); }
		} catch (fetchError) { showFlashError("Erreur de connexion"); }
	});
}

/**
 *	Supprime un utilisateur apres confirmation.
 *	DELETE /api/admin/users/:id
 *
 *	@param userId Identifiant de l utilisateur
 */
function deleteUser(userId) {
	if (!confirm("Supprimer cet utilisateur ?")) return;
	fetch("/api/admin/users/" + userId, { method: "DELETE" })
		.then(function(response) {
			if (response.ok) { window.location.href = "/admin/users"; }
			else { showFlashError("Erreur lors de la suppression"); }
		})
		.catch(function() { showFlashError("Erreur de connexion"); });
}

// ------------------------------------------------------------------------------
// Profil utilisateur
// ------------------------------------------------------------------------------

/**
 *	Gere la modification du profil utilisateur.
 *	#profile-edit-form -> PATCH /api/users/:id
 */
function initProfileEditForm() {
	const form = document.getElementById("profile-edit-form");
	if (!form) return;
	const userId = form.dataset.id;
	form.addEventListener("submit", async function(submitEvent) {
		submitEvent.preventDefault();
		const body = {
			name: document.getElementById("profile-name").value,
			email: document.getElementById("profile-email").value,
		};
		try {
			const response = await fetch("/api/users/" + userId, {
				method: "PATCH",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify(body),
			});
			if (response.ok) { window.location.href = "/users/" + userId; }
			else { showFlashError("Erreur lors de la modification"); }
		} catch (fetchError) { showFlashError("Erreur de connexion"); }
	});
}

// ==============================================================================
// Main
// ==============================================================================

/**
 *	Initialise tous les handlers au chargement de la page.
 */
function main() {
	capitalizeDisplayName();
	updateNavbarCount();
	renderCart();
	renderOrderReview();
	initLoginForm();
	initRegisterForm();
	initConfirmOrder();
	initPlantForm();
	initPlantEditForm();
	initUserEditForm();
	initProfileEditForm();
	window.addEventListener("cart-updated", updateNavbarCount);
	window.addEventListener("storage", updateNavbarCount);
}

// ==============================================================================
// Lancement du programme
// ==============================================================================

if (document.readyState === "loading") {
	document.addEventListener("DOMContentLoaded", main);
} else {
	main();
}