# 🌿 Plant Shop Play Framework – Java MVC

Application e-commerce Java complète avec **Play Framework 3.0**, templates **Twirl** et ORM **Ebean**. Le projet démontre l'utilisation de Play Java pour une application web avec authentification JWT, panier côté client et administration CRUD.

---

## 🎯 Objectif

* Démontrer Play Framework 3.0 en Java avec l'écosystème Pekko (fork Apache d'Akka).
* Illustrer l'ORM Ebean (approche sessionless, différente de JPA/Hibernate).
* Comparer avec les autres Plant Shop (Spring Boot, Django, Rails, etc.) sur un cas identique.

---

## 🛠 Stack utilisée

* Play Framework 3.0 + Pekko
* Java 21
* Ebean ORM + PostgreSQL 42.7.4
* bcrypt (at.favre.lib 0.10.2) pour le hachage des mots de passe
* JWT (jjwt 0.12.6) + cookie httpOnly pour l'authentification
* Templates Twirl (HTML côté serveur)
* Bootstrap 5.3.2 via CDN
* JavaScript vanilla pour le panier (localStorage)

---

## 📦 Structure du repo

    plant_shop_play_framework/
    ├── app/
    │   ├── controllers/            → AuthController, PlantsController, OrdersController,
    │   │                             UsersController, AdminPlantsController, AdminUsersController,
    │   │                             PagesController, HomeController
    │   ├── models/                 → User, Plant, Order, OrderItem (Ebean)
    │   ├── views/                  → Templates Twirl (main, plants, cart, orders, auth, admin, users)
    │   ├── security/               → AuthAction (JWT, bcrypt, cookie)
    │   └── seed/                   → Seed.java
    ├── conf/
    │   ├── application.conf        → Configuration Play + BDD + Ebean
    │   ├── routes                  → Définition des routes (API + pages)
    │   └── evolutions/default/     → Migrations SQL
    ├── public/
    │   ├── javascripts/main.js     → Panier, auth, CRUD (vanilla JS)
    │   └── stylesheets/main.css    → CSS custom (navbar verte)
    ├── test/                       → TestE2E.java
    ├── build.sbt                   → Dépendances SBT
    └── Makefile                    → Cibles de lancement

---

## 🧰 Cibles Makefile

| Catégorie | Cible | Description |
| --- | --- | --- |
| Exécution | make run | Lance le binaire Play (port 4500) |
| Build | make compile | Compile via sbt stage |
| Build | make compile_run | Compile puis lance |
| Seed | make seed | Exécute la seed Java |
| Seed | make seed-build | Compile la seed sans exécuter |
| Seed | make seed-dev | Force recompilation + exécute |
| Tests | make tests | Lance les tests e2e Java |
| Tests | make test-build | Compile les tests sans exécuter |
| Tests | make test-dev | Force recompilation + exécute |
| BDD | make db-create | Crée la base PostgreSQL |
| BDD | make db-migrate | Applique les evolutions SQL |
| BDD | make db-reset | Drop + create + migrate + seed |

---

## 🧩 Fonctionnalités

* Catalogue des plantes avec grille Bootstrap
* Panier persistant côté client (localStorage + debounce 300ms)
* Tunnel de commande avec vérification du stock
* Historique des commandes avec statuts traduits en français
* Profil utilisateur avec édition
* Administration des plantes et utilisateurs (CRUD complet)
* Dropdown admin dans la navbar
* Capitalisation des noms (exception de)
* Gestion EADDRINUSE (vérification port avant démarrage Guice/HikariCP)

---

## 🛡️ Sécurité

* Authentification JWT cookie httpOnly (SameSite Lax)
* Hachage bcrypt (cost 12)
* Routes protégées : redirection 302 vers /auth/signin (jamais de 403/404)
* Admin uniquement : CRUD plantes et utilisateurs
* CSRF désactivé par route API (annotation + nocsrf dans routes)

---

## 🚀 Installation

Prérequis : Java 21 + SBT

make db-create
make compile
make seed
make run
