/* =====================================================
   DRIVE EASE - COMMON JAVASCRIPT (talks to the backend)
   Every page loads this file; it works out which page
   it is on and wires up that page.
===================================================== */

const PROTECTED_PAGES = ["dashboard.htm", "bookings.htm", "my_bookings.htm", "profile.htm"];
let currentUser = null;


/* ================= API HELPER ================= */

async function api(path, options = {}) {
    const method = options.method || "GET";
    const init = { method, credentials: "same-origin", headers: {} };

    if (method !== "GET") {
        init.headers["Content-Type"] = "application/json";
        init.body = JSON.stringify(options.body || {});
    }

    let response;
    try {
        response = await fetch("/api" + path, init);
    } catch (e) {
        throw new Error("Can't reach the server. Is the backend running?");
    }

    let data = null;
    try { data = await response.json(); } catch (e) { /* no body */ }

    if (!response.ok) {
        const error = new Error((data && data.error) || "Something went wrong");
        error.status = response.status;
        throw error;
    }
    return data;
}


/* ================= SMALL HELPERS ================= */

function esc(value) {
    const div = document.createElement("div");
    div.textContent = value == null ? "" : String(value);
    return div.innerHTML;
}

function formatINR(amount) {
    return "₹" + Number(amount).toLocaleString("en-IN");
}

function formatDate(isoDate) {
    return new Date(isoDate + "T00:00:00").toLocaleDateString("en-GB", {
        day: "2-digit", month: "short", year: "numeric"
    });
}

function currentPage() {
    return location.pathname.split("/").pop() || "index.htm";
}

function safeNextUrl() {
    /* Only allow redirects to our own pages, e.g. "bookings.htm?car=2" */
    const next = new URLSearchParams(location.search).get("next");
    return next && /^[a-z_]+\.htm(\?[\w=&%.-]*)?$/i.test(next) ? next : "dashboard.htm";
}

function showError(form, message) {
    let box = form.querySelector(".form-error");
    if (!box) {
        box = document.createElement("p");
        box.className = "form-error";
        box.setAttribute("role", "alert");
        const submit = form.querySelector('button[type="submit"]');
        form.insertBefore(box, submit);
    }
    box.textContent = message;
}

function clearError(form) {
    const box = form.querySelector(".form-error");
    if (box) box.remove();
}

function setBusy(button, busy, busyText) {
    if (!button) return;
    if (busy) {
        button.dataset.label = button.textContent;
        button.textContent = busyText || "Please wait…";
        button.disabled = true;
    } else {
        button.textContent = button.dataset.label || button.textContent;
        button.disabled = false;
    }
}

function injectStyles() {
    const css = `
        .form-error { color:#b3261e; background:#fdecea; border:1px solid #f5c2be;
            padding:10px 14px; font-size:14px; margin:0 0 16px; }
        .cars-status { grid-column:1/-1; color:#666666; padding:40px 0; text-align:center; }
        .empty-note { color:#666666; padding:24px 0; }
        .de-modal-backdrop { position:fixed; inset:0; background:rgba(17,17,17,.55);
            display:flex; align-items:center; justify-content:center; z-index:1000; padding:20px; }
        .de-modal { background:#f5f0e8; border:1.5px solid #111111; width:100%; max-width:420px; padding:32px; }
        .de-modal h3 { font-family:"Playfair Display",serif; font-size:26px; margin:0 0 20px; }
        .de-modal label { display:block; font-size:13px; font-weight:600; margin:14px 0 6px; }
        .de-modal input { width:100%; height:46px; padding:0 14px; border:1.5px solid #111111;
            background:#ffffff; font-family:inherit; font-size:14px; box-sizing:border-box; }
        .de-modal-actions { display:flex; gap:12px; margin-top:24px; }
        .de-modal-actions button { flex:1; height:46px; border:1.5px solid #111111; cursor:pointer;
            font-family:inherit; font-size:14px; background:transparent; color:#111111; }
        .de-modal-actions button.primary { background:#111111; color:#ffffff; }
        .de-modal .form-error { margin:16px 0 0; }
    `;
    const style = document.createElement("style");
    style.textContent = css;
    document.head.appendChild(style);
}


/* ================= USER NAME ================= */

function getUserName() {
    return currentUser ? currentUser.name : "User";
}

function displayUserName() {
    const userName = getUserName();

    const dashboardName = document.getElementById("userName");
    if (dashboardName) dashboardName.textContent = userName + ".";

    const profileName = document.getElementById("profileName");
    if (profileName) profileName.textContent = userName;

    const displayName = document.getElementById("displayName");
    if (displayName) displayName.textContent = userName;

    const avatarLetter = document.getElementById("avatarLetter");
    if (avatarLetter) avatarLetter.textContent = userName.charAt(0).toUpperCase();
}


/* ================= LOGOUT ================= */

async function logout() {
    try { await api("/logout", { method: "POST" }); } catch (e) { /* ignore */ }
    window.location.href = "login.htm";
}

function wireLogoutLinks() {
    document.querySelectorAll("a").forEach(function (link) {
        if (link.textContent.trim().toLowerCase() === "logout") {
            link.addEventListener("click", function (event) {
                event.preventDefault();
                logout();
            });
        }
    });
}

/* On public pages, show Profile / Logout instead of Login / Register once signed in */
function swapNavForLoggedInUser() {
    const navRight = document.querySelector(".nav-right");
    if (!navRight || !currentUser || !navRight.querySelector(".login-btn")) return;

    navRight.innerHTML =
        '<a href="profile.htm" class="profile-link">Profile</a>' +
        '<a href="login.htm" class="register-btn">Logout</a>';
}


/* ================= LOGIN ================= */

function initLogin() {
    const form = document.querySelector("form");
    if (!form) return;

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        clearError(form);
        const button = form.querySelector('button[type="submit"]');
        setBusy(button, true, "Signing in…");
        try {
            await api("/login", {
                method: "POST",
                body: {
                    email: document.getElementById("email").value,
                    password: document.getElementById("password").value,
                    remember: document.getElementById("remember").checked
                }
            });
            window.location.href = safeNextUrl();
        } catch (error) {
            showError(form, error.message);
            setBusy(button, false);
        }
    });
}


/* ================= REGISTER ================= */

async function registerUser(event) {
    event.preventDefault();
    const form = event.target;
    clearError(form);

    const password = document.getElementById("password").value;
    if (password !== document.getElementById("confirm-password").value) {
        showError(form, "Passwords do not match");
        return;
    }

    const button = form.querySelector('button[type="submit"]');
    setBusy(button, true, "Creating account…");
    try {
        await api("/register", {
            method: "POST",
            body: {
                name: document.getElementById("name").value,
                email: document.getElementById("email").value,
                password: password
            }
        });
        window.location.href = "dashboard.htm";   /* registering also signs you in */
    } catch (error) {
        showError(form, error.message);
        setBusy(button, false);
    }
}


/* ================= CARS PAGE ================= */

function carCardHTML(car) {
    return `
        <div class="car-card">
            <div class="car-image">
                <span>CAR IMAGE</span>
                ${car.popular ? '<div class="car-tag">POPULAR</div>' : ""}
            </div>
            <div class="car-details">
                <p class="car-category">${esc(car.category.toUpperCase())}</p>
                <h2>${esc(car.name)}</h2>
                <p class="car-description">${esc(car.description)}</p>
                <div class="car-specs">
                    <span>${car.seats} Seats</span>
                    <span>${esc(car.transmission)}</span>
                    <span>${esc(car.fuel)}</span>
                </div>
                <div class="car-price-row">
                    <div>
                        <strong>${formatINR(car.pricePerDay)}</strong>
                        <span>/ day</span>
                    </div>
                    <a href="bookings.htm?car=${car.id}" class="book-car-btn">Book →</a>
                </div>
            </div>
        </div>`;
}

function initCars() {
    const grid = document.querySelector(".cars-grid");
    const sortSelect = document.getElementById("sort");
    const filterButtons = document.querySelectorAll(".filter-btn");
    if (!grid) return;

    const sortValues = ["recommended", "price_asc", "price_desc", "name"];
    let category = "All Cars";

    async function load() {
        grid.innerHTML = '<p class="cars-status">Loading cars…</p>';
        const params = new URLSearchParams({
            category: category,
            sort: sortValues[sortSelect.selectedIndex] || "recommended"
        });
        try {
            const data = await api("/cars?" + params);
            grid.innerHTML = data.cars.length
                ? data.cars.map(carCardHTML).join("")
                : '<p class="cars-status">No cars found in this category.</p>';
        } catch (error) {
            grid.innerHTML = '<p class="cars-status">' + esc(error.message) + "</p>";
        }
    }

    filterButtons.forEach(function (button) {
        button.addEventListener("click", function () {
            filterButtons.forEach(function (b) { b.classList.remove("active-filter"); });
            button.classList.add("active-filter");
            category = button.textContent.trim();
            load();
        });
    });
    sortSelect.addEventListener("change", load);
    load();
}


/* ================= BOOKING PAGE ================= */

async function initBooking() {
    const carId = new URLSearchParams(location.search).get("car");
    if (!carId) { window.location.href = "cars.htm"; return; }

    let car;
    try {
        car = (await api("/cars/" + encodeURIComponent(carId))).car;
    } catch (error) {
        window.location.href = "cars.htm";
        return;
    }

    /* Fill the car card + summary with real data */
    const details = document.querySelector(".booking-car-details");
    details.querySelector(".car-category").textContent = car.category.toUpperCase();
    details.querySelector("h2").textContent = car.name;
    details.querySelector(".booking-car-description").textContent = car.description;
    const specs = details.querySelectorAll(".booking-specs strong");
    specs[0].textContent = car.seats;
    specs[1].textContent = car.transmission;
    specs[2].textContent = car.fuel;
    details.querySelector(".booking-price span").textContent = formatINR(car.pricePerDay);

    const summary = document.querySelectorAll(".booking-summary strong");
    summary[0].textContent = car.name;
    summary[1].textContent = formatINR(car.pricePerDay) + " / day";
    document.title = "Book " + car.name + " | DriveEase";

    const form = document.getElementById("bookingForm");
    const pickupDate = document.getElementById("pickupDate");
    const returnDate = document.getElementById("returnDate");
    const totalAmount = document.getElementById("totalAmount");

    const today = new Date();
    const todayIso = new Date(today.getTime() - today.getTimezoneOffset() * 60000)
        .toISOString().split("T")[0];
    pickupDate.min = todayIso;
    returnDate.min = todayIso;

    function rentalDays() {
        if (!pickupDate.value || !returnDate.value) return 0;
        return Math.round((new Date(returnDate.value) - new Date(pickupDate.value)) / 86400000);
    }

    function calculateTotal() {
        const days = rentalDays();
        totalAmount.textContent = formatINR(days > 0 ? days * car.pricePerDay : car.pricePerDay);
    }

    pickupDate.addEventListener("change", function () {
        if (pickupDate.value) returnDate.min = pickupDate.value;
        calculateTotal();
    });
    returnDate.addEventListener("change", calculateTotal);
    calculateTotal();

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        clearError(form);

        if (rentalDays() < 1) {
            showError(form, "Return date must be after the pickup date.");
            return;
        }

        const button = form.querySelector('button[type="submit"]');
        setBusy(button, true, "Booking…");
        try {
            await api("/bookings", {
                method: "POST",
                body: {
                    carId: car.id,
                    pickupDate: pickupDate.value,
                    returnDate: returnDate.value,
                    pickupLocation: document.getElementById("pickupLocation").value,
                    dropLocation: document.getElementById("dropLocation").value
                }
            });
            alert("Booking confirmed successfully!");
            window.location.href = "my_bookings.htm";
        } catch (error) {
            if (error.status === 401) { window.location.href = "login.htm"; return; }
            showError(form, error.message);
            setBusy(button, false);
        }
    });
}


/* ================= MY BOOKINGS PAGE ================= */

function upcomingCardHTML(b) {
    return `
        <div class="my-booking-card">
            <div class="my-booking-image"><span>CAR IMAGE</span></div>
            <div class="my-booking-details">
                <div>
                    <p class="car-category">${esc(b.car.category.toUpperCase())}</p>
                    <h2>${esc(b.car.name)}</h2>
                </div>
                <div class="booking-info-grid">
                    <div>
                        <span>PICKUP</span>
                        <strong>${formatDate(b.pickupDate)}</strong>
                        <small>${esc(b.pickupLocation)}</small>
                    </div>
                    <div>
                        <span>RETURN</span>
                        <strong>${formatDate(b.returnDate)}</strong>
                        <small>${esc(b.dropLocation)}</small>
                    </div>
                    <div>
                        <span>TOTAL</span>
                        <strong>${formatINR(b.totalPrice)}</strong>
                    </div>
                </div>
                <div class="booking-actions">
                    <button class="outline-btn" onclick="cancelBooking(${b.id})">Cancel Booking</button>
                    <button class="primary-small-btn" onclick="viewBooking(${b.id})">View Details →</button>
                </div>
            </div>
        </div>`;
}

function pastCardHTML(b) {
    const label = b.status === "cancelled" ? "CANCELLED" : "COMPLETED";
    return `
        <div class="past-booking-card">
            <div class="past-car-image"><span>CAR IMAGE</span></div>
            <div class="past-booking-info">
                <div>
                    <p class="car-category">${esc(b.car.category.toUpperCase())}</p>
                    <h3>${esc(b.car.name)}</h3>
                </div>
                <div class="past-booking-date">
                    <span>${formatDate(b.pickupDate).toUpperCase()}</span>
                    <strong>${formatINR(b.totalPrice)}</strong>
                </div>
            </div>
            <span class="completed-status">${label}</span>
        </div>`;
}

let loadedBookings = [];

async function loadMyBookings() {
    const upcomingSection = document.querySelector(".bookings-section");
    const pastContainer = document.querySelector(".past-bookings");
    let data;
    try {
        data = await api("/bookings");
    } catch (error) {
        if (error.status === 401) { window.location.href = "login.htm"; return; }
        upcomingSection.insertAdjacentHTML("beforeend",
            '<p class="empty-note">' + esc(error.message) + "</p>");
        return;
    }
    loadedBookings = data.bookings;

    /* Upcoming */
    let list = document.getElementById("upcomingList");
    if (!list) {
        const oldCard = document.getElementById("bookingCard");
        list = document.createElement("div");
        list.id = "upcomingList";
        oldCard.replaceWith(list);
    }
    list.innerHTML = data.upcoming.length
        ? data.upcoming.map(upcomingCardHTML).join("")
        : '<p class="empty-note">You have no upcoming bookings.</p>';

    const badge = upcomingSection.querySelector(".booking-status");
    if (badge) badge.style.display = data.upcoming.length ? "" : "none";

    /* Past + cancelled */
    pastContainer.innerHTML = data.past.length
        ? data.past.map(pastCardHTML).join("")
        : '<p class="empty-note">No past bookings yet.</p>';
}

async function cancelBooking(id) {
    if (!confirm("Are you sure you want to cancel this booking?")) return;
    try {
        await api("/bookings/" + id + "/cancel", { method: "POST" });
        alert("Booking cancelled successfully.");
        loadMyBookings();
    } catch (error) {
        alert(error.message);
    }
}

function viewBooking(id) {
    const b = loadedBookings.find(function (item) { return item.id === id; });
    if (!b) return;
    alert(
        "Booking " + b.reference + "\n\n" +
        b.car.name + " (" + b.car.category + ")\n" +
        "Pickup: " + formatDate(b.pickupDate) + ", " + b.pickupLocation + "\n" +
        "Return: " + formatDate(b.returnDate) + ", " + b.dropLocation + "\n" +
        b.days + " day(s) x " + formatINR(b.car.pricePerDay) + "\n" +
        "Total: " + formatINR(b.totalPrice)
    );
}


/* ================= DASHBOARD PAGE ================= */

async function initDashboard() {
    let data;
    try {
        data = await api("/dashboard");
    } catch (error) {
        if (error.status === 401) window.location.href = "login.htm";
        return;
    }

    /* Stats */
    const pad = function (n) { return String(n).padStart(2, "0"); };
    const numbers = document.querySelectorAll(".dashboard-stat .stat-number");
    const values = [data.stats.total, data.stats.active, data.stats.completed, data.stats.cancelled];
    numbers.forEach(function (el, i) { el.textContent = pad(values[i]); });

    /* Current booking */
    const card = document.querySelector(".current-booking .booking-card");
    const b = data.currentBooking;
    if (b && card) {
        card.querySelector(".booking-info h3").textContent = b.car.name;
        card.querySelector(".booking-details").textContent =
            b.car.transmission + "  •  " + b.car.fuel + "  •  " + b.car.seats + " Seats";
        const dates = card.querySelectorAll(".booking-dates strong");
        const places = card.querySelectorAll(".booking-dates p");
        dates[0].textContent = formatDate(b.pickupDate).toUpperCase();
        dates[1].textContent = formatDate(b.returnDate).toUpperCase();
        places[0].textContent = b.pickupLocation;
        places[1].textContent = b.dropLocation;
        card.querySelector(".booking-price strong").textContent = formatINR(b.totalPrice);
    } else if (card) {
        card.outerHTML =
            '<p class="empty-note">No upcoming rides. <a href="cars.htm">Find a car →</a></p>';
    }

    /* Featured cars */
    const grid = document.querySelector(".dashboard-car-grid");
    if (grid) {
        grid.innerHTML = data.featuredCars.map(function (car) {
            return `
                <div class="dashboard-car-card">
                    <div class="dashboard-car-image"><span>CAR IMAGE</span></div>
                    <div class="dashboard-car-info">
                        <p class="car-type">${esc(car.category.toUpperCase())}</p>
                        <h3>${esc(car.name)}</h3>
                        <div class="car-bottom">
                            <span>${formatINR(car.pricePerDay)}/day</span>
                            <a href="bookings.htm?car=${car.id}">Book →</a>
                        </div>
                    </div>
                </div>`;
        }).join("");
    }
}


/* ================= PROFILE PAGE ================= */

function renderProfile() {
    displayUserName();
    const items = document.querySelectorAll(".profile-info-item strong");
    if (items.length < 4 || !currentUser) return;
    items[0].textContent = currentUser.name;
    items[1].textContent = currentUser.email;
    items[2].textContent = currentUser.phone || "Not added";
    items[3].textContent = new Date(currentUser.createdAt.replace(" ", "T") + "Z")
        .toLocaleDateString("en-US", { month: "long", year: "numeric" });
}

async function editProfile() {
    const name = prompt("Enter your full name:", currentUser.name);
    if (name === null) return;
    const email = prompt("Enter your email address:", currentUser.email);
    if (email === null) return;
    const phone = prompt("Enter your phone number (leave blank to remove):", currentUser.phone || "");
    if (phone === null) return;

    try {
        currentUser = (await api("/me", { method: "PUT", body: { name, email, phone } })).user;
        renderProfile();
    } catch (error) {
        alert(error.message);
    }
}

function changePassword() {
    const backdrop = document.createElement("div");
    backdrop.className = "de-modal-backdrop";
    backdrop.innerHTML = `
        <form class="de-modal">
            <h3>Change password</h3>
            <label for="curPw">Current password</label>
            <input type="password" id="curPw" autocomplete="current-password" required>
            <label for="newPw">New password (min. 8 characters)</label>
            <input type="password" id="newPw" autocomplete="new-password" minlength="8" required>
            <label for="newPw2">Confirm new password</label>
            <input type="password" id="newPw2" autocomplete="new-password" minlength="8" required>
            <div class="de-modal-actions">
                <button type="button" id="pwCancel">Cancel</button>
                <button type="submit" class="primary">Update</button>
            </div>
        </form>`;
    document.body.appendChild(backdrop);

    const form = backdrop.querySelector("form");
    backdrop.querySelector("#pwCancel").onclick = function () { backdrop.remove(); };

    form.addEventListener("submit", async function (event) {
        event.preventDefault();
        clearError(form);
        const newPw = form.querySelector("#newPw").value;
        if (newPw !== form.querySelector("#newPw2").value) {
            return showError(form, "New passwords do not match");
        }
        try {
            await api("/me/password", {
                method: "PUT",
                body: { currentPassword: form.querySelector("#curPw").value, newPassword: newPw }
            });
            backdrop.remove();
            alert("Password updated successfully.");
        } catch (error) {
            showError(form, error.message);
        }
    });
}

function contactSupport() {
    alert("DriveEase Support: support@driveease.com");
}


/* ================= PAGE LOAD ================= */

document.addEventListener("DOMContentLoaded", async function () {
    injectStyles();
    const page = currentPage();

    try {
        currentUser = (await api("/me")).user;
    } catch (error) {
        currentUser = null;
    }

    if (PROTECTED_PAGES.includes(page) && !currentUser) {
        window.location.href = "login.htm";
        return;
    }
    if ((page === "login.htm" || page === "register.htm") && currentUser) {
        window.location.href = "dashboard.htm";
        return;
    }

    swapNavForLoggedInUser();
    displayUserName();
    wireLogoutLinks();

    if (page === "login.htm") initLogin();
    if (page === "cars.htm") initCars();
    if (page === "bookings.htm") initBooking();
    if (page === "my_bookings.htm") loadMyBookings();
    if (page === "dashboard.htm") initDashboard();
    if (page === "profile.htm") renderProfile();
});
