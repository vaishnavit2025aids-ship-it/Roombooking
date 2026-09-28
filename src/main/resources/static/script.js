const API = "";

let currentPage = "dashboard";

let editingRoomId = null;
let editingEmployeeId = null;
let editingBookingId = null;

const $ = id => document.getElementById(id);


/* =======================================================
   COMMON
========================================= */

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}


function showMessage(message, type = "success") {
    const box = $("message");

    if (!box) return;

    box.textContent = message;
    box.className = `message ${type}`;

    window.scrollTo({
        top: 0,
        behavior: "smooth"
    });
}


function hideMessage() {
    const box = $("message");

    if (box) {
        box.className = "message hidden";
    }
}


function badge(status) {
    const css = String(status ?? "")
        .toLowerCase()
        .replaceAll(" ", "_");

    return `
        <span class="badge ${css}">
            ${escapeHtml(status)}
        </span>
    `;
}


function formatDate(value) {
    if (!value) {
        return "-";
    }

    return new Date(value).toLocaleString();
}


function formatForInput(value) {
    if (!value) {
        return "";
    }

    return value.substring(0, 16);
}


/* =========================================
   API REQUEST
========================================= */

async function request(
    path,
    method = "GET",
    body = null
) {

    const options = {
        method,
        headers: {}
    };

    if (body !== null) {
        options.headers["Content-Type"] =
            "application/json";

        options.body =
            JSON.stringify(body);
    }

    let response;

    try {
        response =
            await fetch(API + path, options);

    } catch {
        throw new Error(
            "Cannot connect to Spring Boot server."
        );
    }

    const text =
        await response.text();

    let data = null;

    if (text) {
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }
    }

    if (!response.ok) {

        if (
            data &&
            typeof data === "object"
        ) {

            if (data.error) {
                throw new Error(data.error);
            }

            const messages =
                Object.values(data);

            if (messages.length > 0) {
                throw new Error(
                    messages.join(", ")
                );
            }
        }

        throw new Error(
            typeof data === "string"
                ? data
                : `Request failed (${response.status})`
        );
    }

    return data;
}


/* =========================================
   TABLE
========================================= */

function renderTable(
    containerId,
    rows,
    columns,
    actionFunction = null
) {

    const container =
        $(containerId);

    if (!container) {
        return;
    }

    if (!rows || rows.length === 0) {

        container.innerHTML = `
            <div class="empty">
                No records found.
            </div>
        `;

        return;
    }

    let html = `
        <div class="table-wrap">
        <table>

        <thead>
        <tr>
    `;

    columns.forEach(column => {

        html += `
            <th>
                ${escapeHtml(column.label)}
            </th>
        `;
    });

    if (actionFunction) {
        html += `<th>Action</th>`;
    }

    html += `
        </tr>
        </thead>

        <tbody>
    `;

    rows.forEach(row => {

        html += "<tr>";

        columns.forEach(column => {

            let value;

            if (column.value) {
                value = column.value(row);
            } else {
                value = row[column.key];
            }

            if (column.format === "badge") {

                value = badge(value);

            } else if (
                column.format === "date"
            ) {

                value =
                    escapeHtml(
                        formatDate(value)
                    );

            } else {

                value =
                    escapeHtml(value);
            }

            html += `
                <td>
                    ${value}
                </td>
            `;
        });

        if (actionFunction) {

            html += `
                <td>
                    ${actionFunction(row)}
                </td>
            `;
        }

        html += "</tr>";
    });

    html += `
        </tbody>
        </table>
        </div>
    `;

    container.innerHTML = html;
}


/* =========================================
   NAVIGATION
========================================= */

document
    .querySelectorAll(".nav-btn")
    .forEach(button => {

        button.addEventListener(
            "click",
            async () => {

                document
                    .querySelectorAll(".nav-btn")
                    .forEach(btn =>
                        btn.classList.remove("active")
                    );

                document
                    .querySelectorAll(".page")
                    .forEach(page =>
                        page.classList.remove("active")
                    );

                button.classList.add("active");

                currentPage =
                    button.dataset.page;

                const page =
                    $(currentPage);

                if (page) {
                    page.classList.add("active");
                }

                const title =
                    $("pageTitle");

                if (title) {
                    title.textContent =
                        button.textContent.trim();
                }

                hideMessage();

                await loadPage(currentPage);
            }
        );
    });


const refreshBtn =
    $("refreshBtn");

if (refreshBtn) {

    refreshBtn.addEventListener(
        "click",
        async () => {

            await loadPage(
                currentPage
            );
        }
    );
}


/* =========================================
   DASHBOARD
========================================= */

async function loadDashboard() {

    const [
        rooms,
        employees,
        bookings
    ] = await Promise.all([

        request("/rooms"),
        request("/employees"),
        request("/bookings")
    ]);

    $("totalRooms").textContent =
        rooms.length;

    $("totalEmployees").textContent =
        employees.length;

    $("totalBookings").textContent =
        bookings.length;

    $("confirmedBookings").textContent =
        bookings.filter(
            booking =>
                booking.status === "CONFIRMED"
        ).length;

    const recent =
        [...bookings]
            .sort(
                (a, b) =>
                    Number(b.id) -
                    Number(a.id)
            )
            .slice(0, 5);

    renderTable(
        "dashboardBookings",
        recent,

        [
            {
                key: "id",
                label: "ID"
            },

            {
                label: "Room",
                value: booking =>
                    booking.room?.roomName
            },

            {
                label: "Employee",
                value: booking =>
                    booking.employee?.name
            },

            {
                key: "startTime",
                label: "Start",
                format: "date"
            },

            {
                key: "status",
                label: "Status",
                format: "badge"
            }
        ]
    );
}


/* =========================================
   ROOM CRUD
========================================= */

async function loadRooms() {

    const rooms =
        await request("/rooms");

    renderTable(
        "roomsTable",
        rooms,

        [
            {
                key: "id",
                label: "ID"
            },

            {
                key: "roomName",
                label: "Room Name"
            },

            {
                key: "capacity",
                label: "Capacity"
            },

            {
                label: "Projector",
                value: room =>
                    room.projector
                        ? "Yes"
                        : "No"
            },

            {
                label: "Whiteboard",
                value: room =>
                    room.whiteboard
                        ? "Yes"
                        : "No"
            }
        ],

        room => `

            <button
                class="edit-btn"
                onclick="editRoom(${room.id})">

                Edit

            </button>

            <button
                class="delete-btn"
                onclick="deleteRoom(${room.id})">

                Delete

            </button>
        `
    );
}


const roomForm =
    $("roomForm");

if (roomForm) {

    roomForm.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            const form =
                event.target;

            const data = {

                roomName:
                form.elements[
                    "roomName"
                    ].value,

                capacity:
                    Number(
                        form.elements[
                            "capacity"
                            ].value
                    ),

                projector:
                form.elements[
                    "projector"
                    ].checked,

                whiteboard:
                form.elements[
                    "whiteboard"
                    ].checked
            };

            try {

                if (
                    editingRoomId === null
                ) {

                    await request(
                        "/rooms",
                        "POST",
                        data
                    );

                    showMessage(
                        "Room added successfully!"
                    );

                } else {

                    await request(
                        `/rooms/${editingRoomId}`,
                        "PUT",
                        data
                    );

                    showMessage(
                        "Room updated successfully!"
                    );
                }

                resetRoomForm();

                await loadRooms();

            } catch (error) {

                showMessage(
                    error.message,
                    "error"
                );
            }
        }
    );
}


async function editRoom(id) {

    try {

        const room =
            await request(
                `/rooms/${id}`
            );

        const form =
            $("roomForm");

        form.elements[
            "roomName"
            ].value =
            room.roomName;

        form.elements[
            "capacity"
            ].value =
            room.capacity;

        form.elements[
            "projector"
            ].checked =
            room.projector;

        form.elements[
            "whiteboard"
            ].checked =
            room.whiteboard;

        editingRoomId = id;

        form.querySelector(
            'button[type="submit"]'
        ).textContent =
            "Update Room";

        $("cancelRoomEdit")
            .classList.remove("hidden");

        form.scrollIntoView({
            behavior: "smooth"
        });

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


async function deleteRoom(id) {

    if (
        !confirm(
            "Delete this room?"
        )
    ) {
        return;
    }

    try {

        await request(
            `/rooms/${id}`,
            "DELETE"
        );

        showMessage(
            "Room deleted successfully!"
        );

        await loadRooms();

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


function resetRoomForm() {

    const form =
        $("roomForm");

    form.reset();

    editingRoomId = null;

    form.querySelector(
        'button[type="submit"]'
    ).textContent =
        "Add Room";

    $("cancelRoomEdit")
        .classList.add("hidden");
}


const cancelRoomEdit =
    $("cancelRoomEdit");

if (cancelRoomEdit) {

    cancelRoomEdit.addEventListener(
        "click",
        resetRoomForm
    );
}


/* =========================================
   EMPLOYEE CRUD
========================================= */

async function loadEmployees() {

    const employees =
        await request(
            "/employees"
        );

    renderTable(
        "employeesTable",
        employees,

        [
            {
                key: "id",
                label: "ID"
            },

            {
                key: "name",
                label: "Name"
            },

            {
                key: "email",
                label: "Email"
            },

            {
                key: "department",
                label: "Department"
            }
        ],

        employee => `

            <button
                class="edit-btn"
                onclick="editEmployee(${employee.id})">

                Edit

            </button>

            <button
                class="delete-btn"
                onclick="deleteEmployee(${employee.id})">

                Delete

            </button>
        `
    );
}


const employeeForm =
    $("employeeForm");

if (employeeForm) {

    employeeForm.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            const form =
                event.target;

            const data = {

                name:
                form.elements[
                    "name"
                    ].value,

                email:
                form.elements[
                    "email"
                    ].value,

                department:
                form.elements[
                    "department"
                    ].value
            };

            try {

                if (
                    editingEmployeeId === null
                ) {

                    await request(
                        "/employees",
                        "POST",
                        data
                    );

                    showMessage(
                        "Employee added successfully!"
                    );

                } else {

                    await request(
                        `/employees/${editingEmployeeId}`,
                        "PUT",
                        data
                    );

                    showMessage(
                        "Employee updated successfully!"
                    );
                }

                resetEmployeeForm();

                await loadEmployees();

            } catch (error) {

                showMessage(
                    error.message,
                    "error"
                );
            }
        }
    );
}


async function editEmployee(id) {

    try {

        const employee =
            await request(
                `/employees/${id}`
            );

        const form =
            $("employeeForm");

        form.elements[
            "name"
            ].value =
            employee.name;

        form.elements[
            "email"
            ].value =
            employee.email;

        form.elements[
            "department"
            ].value =
            employee.department;

        editingEmployeeId =
            id;

        form.querySelector(
            'button[type="submit"]'
        ).textContent =
            "Update Employee";

        $("cancelEmployeeEdit")
            .classList.remove("hidden");

        form.scrollIntoView({
            behavior: "smooth"
        });

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


async function deleteEmployee(id) {

    if (
        !confirm(
            "Delete this employee?"
        )
    ) {
        return;
    }

    try {

        await request(
            `/employees/${id}`,
            "DELETE"
        );

        showMessage(
            "Employee deleted successfully!"
        );

        await loadEmployees();

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


function resetEmployeeForm() {

    const form =
        $("employeeForm");

    form.reset();

    editingEmployeeId = null;

    form.querySelector(
        'button[type="submit"]'
    ).textContent =
        "Add Employee";

    $("cancelEmployeeEdit")
        .classList.add("hidden");
}


const cancelEmployeeEdit =
    $("cancelEmployeeEdit");

if (cancelEmployeeEdit) {

    cancelEmployeeEdit.addEventListener(
        "click",
        resetEmployeeForm
    );
}


/* =========================================
   BOOKING CRUD
========================================= */

async function loadBookings() {

    const [
        rooms,
        employees,
        bookings
    ] = await Promise.all([

        request("/rooms"),
        request("/employees"),
        request("/bookings")
    ]);

    fillSelect(
        "bookingRoom",
        rooms,
        room =>
            `${room.roomName} - Capacity ${room.capacity}`
    );

    fillSelect(
        "bookingEmployee",
        employees,
        employee =>
            `${employee.name} - ${employee.department}`
    );

    renderTable(
        "bookingsTable",
        bookings,

        [
            {
                key: "id",
                label: "ID"
            },

            {
                label: "Room",
                value: booking =>
                    booking.room?.roomName
            },

            {
                label: "Employee",
                value: booking =>
                    booking.employee?.name
            },

            {
                key: "startTime",
                label: "Start",
                format: "date"
            },

            {
                key: "endTime",
                label: "End",
                format: "date"
            },

            {
                key: "status",
                label: "Status",
                format: "badge"
            },

            {
                label: "Check-In",
                value: booking =>
                    booking.checkedIn
                        ? "Yes"
                        : "No"
            }
        ],

        booking => {

            let buttons = `

                <button
                    class="edit-btn"
                    onclick="editBooking(${booking.id})">

                    Edit

                </button>
            `;

            if (
                booking.status ===
                "CONFIRMED"
            ) {

                buttons += `

                    <button
                        class="checkin-btn"
                        onclick="checkInBooking(${booking.id})">

                        Check-In

                    </button>

                    <button
                        class="cancel-btn"
                        onclick="cancelBooking(${booking.id})">

                        Cancel

                    </button>
                `;
            }

            buttons += `

                <button
                    class="delete-btn"
                    onclick="deleteBooking(${booking.id})">

                    Delete

                </button>
            `;

            return buttons;
        }
    );
}


/* =========================================
   FILL SELECT
========================================= */

function fillSelect(
    id,
    rows,
    labelFunction
) {

    const select =
        $(id);

    if (!select) {
        return;
    }

    const currentValue =
        select.value;

    select.innerHTML = `
        <option value="">
            Select
        </option>
    `;

    rows.forEach(row => {

        const option =
            document.createElement(
                "option"
            );

        option.value =
            row.id;

        option.textContent =
            labelFunction(row);

        select.appendChild(
            option
        );
    });

    select.value =
        currentValue;
}


/* =========================================
   CREATE / UPDATE BOOKING
========================================= */

const bookingForm =
    $("bookingForm");

if (bookingForm) {

    bookingForm.addEventListener(
        "submit",
        async event => {

            event.preventDefault();

            const form =
                event.target;

            const data = {

                room: {
                    id:
                        Number(
                            form.elements[
                                "roomId"
                                ].value
                        )
                },

                employee: {
                    id:
                        Number(
                            form.elements[
                                "employeeId"
                                ].value
                        )
                },

                startTime:
                form.elements[
                    "startTime"
                    ].value,

                endTime:
                form.elements[
                    "endTime"
                    ].value
            };

            try {

                if (
                    editingBookingId === null
                ) {

                    const createdBooking =
                        await request(
                            "/bookings",
                            "POST",
                            data
                        );

                    showMessage(
                        "Room booked successfully!"
                    );

                    // 🔔 NOTIFICATION
                    addNotification(
                        `Booking #${createdBooking.id} confirmed successfully.`
                    );

                } else {

                    const updatedBooking =
                        await request(
                            `/bookings/${editingBookingId}`,
                            "PUT",
                            data
                        );

                    showMessage(
                        "Booking updated successfully!"
                    );

                    // 🔔 NOTIFICATION
                    addNotification(
                        `Booking #${updatedBooking.id} updated successfully.`
                    );
                }

                resetBookingForm();

                await loadBookings();

            } catch (error) {

                showMessage(
                    error.message,
                    "error"
                );
            }
        }
    );
}


/* =========================================
   EDIT BOOKING
========================================= */

async function editBooking(id) {

    try {

        const booking =
            await request(
                `/bookings/${id}`
            );

        if (
            booking.status ===
            "CANCELLED" ||
            booking.status ===
            "NO_SHOW"
        ) {

            showMessage(
                "Cancelled or no-show booking cannot be edited.",
                "error"
            );

            return;
        }

        const form =
            $("bookingForm");

        form.elements[
            "roomId"
            ].value =
            booking.room.id;

        form.elements[
            "employeeId"
            ].value =
            booking.employee.id;

        form.elements[
            "startTime"
            ].value =
            formatForInput(
                booking.startTime
            );

        form.elements[
            "endTime"
            ].value =
            formatForInput(
                booking.endTime
            );

        editingBookingId =
            id;

        form.querySelector(
            'button[type="submit"]'
        ).textContent =
            "Update Booking";

        $("cancelBookingEdit")
            .classList.remove("hidden");

        form.scrollIntoView({
            behavior: "smooth"
        });

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


/* =========================================
   DELETE BOOKING
========================================= */

async function deleteBooking(id) {

    if (
        !confirm(
            "Delete this booking?"
        )
    ) {
        return;
    }

    try {

        await request(
            `/bookings/${id}`,
            "DELETE"
        );

        showMessage(
            "Booking deleted successfully!"
        );

        addNotification(
            `Booking #${id} deleted successfully.`
        );

        await loadBookings();

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


/* =========================================
   CANCEL BOOKING
========================================= */

async function cancelBooking(id) {

    if (
        !confirm(
            "Cancel this booking?"
        )
    ) {
        return;
    }

    try {

        const cancelledBooking =
            await request(
                `/bookings/${id}/cancel`,
                "PUT"
            );

        showMessage(
            "Booking cancelled. Time slot is now available."
        );

        // 🔔 NOTIFICATION
        addNotification(
            `Booking #${cancelledBooking.id} cancelled. Time slot is now available.`
        );

        await loadBookings();

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


/* =========================================
   CHECK-IN BOOKING
========================================= */

async function checkInBooking(id) {

    try {

        const checkedBooking =
            await request(
                `/bookings/${id}/checkin`,
                "PUT"
            );

        showMessage(
            "Check-in successful!"
        );

        // 🔔 NOTIFICATION
        addNotification(
            `Booking #${checkedBooking.id} checked in successfully.`
        );

        await loadBookings();

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


/* =========================================
   RESET BOOKING FORM
========================================= */

function resetBookingForm() {

    const form =
        $("bookingForm");

    form.reset();

    editingBookingId =
        null;

    form.querySelector(
        'button[type="submit"]'
    ).textContent =
        "Book Room";

    $("cancelBookingEdit")
        .classList.add("hidden");
}


const cancelBookingEdit =
    $("cancelBookingEdit");

if (cancelBookingEdit) {

    cancelBookingEdit.addEventListener(
        "click",
        resetBookingForm
    );
}


/* =========================================
   PAGE LOADER
========================================= */

async function loadPage(page) {

    try {

        switch (page) {

            case "dashboard":

                await loadDashboard();

                break;


            case "rooms":

                await loadRooms();

                break;


            case "employees":

                await loadEmployees();

                break;


            case "bookings":

                await loadBookings();

                break;
        }

    } catch (error) {

        showMessage(
            error.message,
            "error"
        );
    }
}


/* =========================================
   NOTIFICATION SYSTEM
========================================= */

let notifications = [];

try {

    notifications =
        JSON.parse(
            localStorage.getItem(
                "roombookNotifications"
            )
        ) || [];

} catch {

    notifications = [];
}


/* ADD NOTIFICATION */

function addNotification(message) {

    const notification = {

        message: message,

        time:
            new Date()
                .toLocaleString()
    };

    notifications.unshift(
        notification
    );

    localStorage.setItem(
        "roombookNotifications",
        JSON.stringify(
            notifications
        )
    );

    renderNotifications();
}


/* DISPLAY NOTIFICATIONS */

function renderNotifications() {

    const list =
        $("notificationList");

    const count =
        $("notificationCount");

    if (!list || !count) {
        return;
    }

    count.textContent =
        notifications.length;

    if (
        notifications.length === 0
    ) {

        list.innerHTML = `
            <p class="no-notification">
                No notifications
            </p>
        `;

        return;
    }

    list.innerHTML =
        notifications
            .map(notification => `

                <div class="notification-item">

                    <p>
                        ${escapeHtml(
                notification.message
            )}
                    </p>

                    <span class="notification-time">

                        ${escapeHtml(
                notification.time
            )}

                    </span>

                </div>

            `)
            .join("");
}


/* OPEN / CLOSE NOTIFICATION */

function toggleNotifications() {

    const panel =
        $("notificationPanel");

    if (!panel) {
        return;
    }

    panel.classList.toggle(
        "show"
    );
}


/* CLEAR NOTIFICATIONS */

function clearNotifications() {

    notifications = [];

    localStorage.removeItem(
        "roombookNotifications"
    );

    renderNotifications();
}


/* =========================================
   INITIAL LOAD
========================================= */

document.addEventListener(
    "DOMContentLoaded",
    async () => {

        renderNotifications();

        try {

            await loadDashboard();

        } catch (error) {

            showMessage(
                error.message,
                "error"
            );
        }
    }
);