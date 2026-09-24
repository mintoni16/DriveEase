/* =====================================================
   DRIVE EASE - COMMON JAVASCRIPT
===================================================== */


/* ================= USER NAME ================= */

function getUserName() {

    const savedName = localStorage.getItem("userName");

    return savedName || "User";

}


/* ================= DISPLAY USER NAME ================= */

function displayUserName() {

    const userName = getUserName();


    /* Dashboard */

    const dashboardName =
        document.getElementById("userName");

    if (dashboardName) {

        dashboardName.textContent =
            userName + ".";

    }


    /* Profile */

    const profileName =
        document.getElementById("profileName");

    if (profileName) {

        profileName.textContent =
            userName;

    }


    const displayName =
        document.getElementById("displayName");

    if (displayName) {

        displayName.textContent =
            userName;

    }


    /* Profile avatar */

    const avatarLetter =
        document.getElementById("avatarLetter");

    if (avatarLetter) {

        avatarLetter.textContent =
            userName.charAt(0).toUpperCase();

    }

}


/* ================= LOGOUT ================= */

function logout() {

    localStorage.removeItem("loggedIn");

    window.location.href =
        "login.htm";

}


/* ================= PAGE LOAD ================= */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        displayUserName();

    }
);