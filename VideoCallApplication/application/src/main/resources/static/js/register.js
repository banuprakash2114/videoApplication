document.addEventListener('DOMContentLoaded', function () {
    const registerForm = document.getElementById('registerForm');
    const errorMessage = document.getElementById('errorMessage');
    const loadingContainer = document.getElementById('loadingContainer');

    // Function to show the loading spinner
    function showLoadingSpinner() {
        loadingContainer.style.display = 'block';
    }

    // Function to hide the loading spinner
    function hideLoadingSpinner() {
        loadingContainer.style.display = 'none';
    }

    // Handle form submission
    registerForm.addEventListener('submit', async function (event) {
        event.preventDefault(); // Prevent the default form submission

        // Collect form data
        const formData = {
            username: document.getElementById('username').value,
            email: document.getElementById('email').value,
            password: document.getElementById('password').value,
        };

        try {
            // Show the loading spinner
            showLoadingSpinner();

            // Send POST request to the server
            const response = await fetch(registerForm.action, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json', // Ensure the request is in JSON format
                },
                body: JSON.stringify(formData), // Send data as JSON
            });

            // Check response status
            if (response.ok) {
                // Redirect to login page on success
                window.location.href = 'login.html';
            } else {
                const errorData = await response.text(); // Use .text() first

                try {
                    const parsedError = JSON.parse(errorData); // Attempt to parse it as JSON
                    errorMessage.textContent = parsedError.message || 'Registration failed. Please try again.';
                } catch (e) {
                    errorMessage.textContent = 'An error occurred. Please try again later.';
                }
            }
        } catch (error) {
            // Handle network errors
            errorMessage.textContent = 'An error occurred. Please try again later.';
            console.error('Error:', error);
        } finally {
            // Hide the loading spinner
            hideLoadingSpinner();
        }
    });
});
