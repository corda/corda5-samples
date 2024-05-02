// This script contains functions for making API requests, handling data, and updating the UI.

// Variable to store the selected X500Name from the dropdown
let selectedX500Name;

// Variable to indicate whether data is currently being loaded from the pull request
let loading = false;

// Function to make a GET request to an external API and return the data
function getData() {
    // Replace the URL with the actual API endpoint
    return fetch('https://jsonplaceholder.typicode.com/todos/1')
        .then(response => response.json())
        .then(data => {
            console.log('API Result:', data);

            // Return specific data fields
            return {
                id: data.id,
                title: data.title
            };
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

// Function to get the data and display it on the page
function getDataAndDisplay() {
    getData()
        .then(result => {
            // Update the result div with the data
            document.getElementById('result').innerHTML = `
                <p>ID: ${result.id}</p>
                <p>Title: ${result.title}</p>
            `;
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

// Function to make a GET request to retrieve CPI data
function getCPI() {
    const url = 'https://localhost:8888/api/v1/cpi';
    // Perform the GET request with authorization headers
    return fetch(url, {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Authorization': 'Basic YWRtaW46YWRtaW4='
        }
    })
        .then(response => response.json())
        .then(data => {
            console.log('API Result:', data);
            // Further processing of the data can be done here
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

// Function to get all virtual nodes and populate a dropdown with the data
function getAllVirtualNodes() {
    const url = 'https://localhost:8888/api/v1/virtualnode';

    return fetch(url, {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Authorization': 'Basic YWRtaW46YWRtaW4='
        }
    })
        .then(response => response.json())
        .then(data => {
            // Extract virtualNodes from the API response
            const virtualNodes = data.virtualNodes;
            console.log('API Result:', virtualNodes);

            // Process the data and populate the dropdown
            if (Array.isArray(virtualNodes)) {
                const dropdown = document.getElementById('itemDropdown');
                dropdown.innerHTML = '';
                dropdown.innerHTML += '<option value="" disabled selected>Select your option</option>';

                virtualNodes.forEach(item => {
                    // Display each item on the console
                    console.log('Item X500Name:', item.holdingIdentity.x500Name);
                    console.log('Item ShortHash:', item.holdingIdentity.shortHash);

                    // Create an option element and add it to the dropdown
                    const option = document.createElement('option');
                    option.value = item.holdingIdentity.shortHash;
                    option.text = item.holdingIdentity.x500Name;
                    dropdown.appendChild(option);
                });

                // Add event listener to the dropdown to detect changes
                dropdown.addEventListener('change', function () {
                    selectedX500Name = this.value;
                    console.log('Selected ShortHash:', selectedX500Name);

                    // Call a function or update a variable based on the selected item
                    handleDropdownChange(selectedX500Name);
                });

            } else {
                console.warn('API Result is not an array.');
            }
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

// Initialize the virtual nodes dropdown on page load
getAllVirtualNodes();

// function to handle dropdown change
function handleDropdownChange(selectedX500Name) {
    console.log('Handling dropdown change for Item ID:', selectedX500Name);
    getSelectedVNode();
    // Additional actions based on the selected item can be performed here
}

// Function to get the selected virtual node and display it
function getSelectedVNode() {
    const displayElement = document.getElementById('selectedX500Display');
    displayElement.textContent = `Selected X500: ${selectedX500Name}`;
}

// Function to get all flow results based on the selected virtual node
function getAllFlowResult() {
    const url = `https://localhost:8888/api/v1/flow/${selectedX500Name}`;

    return fetch(url, {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Authorization': 'Basic YWRtaW46YWRtaW4='
        }
    })
        .then(response => response.json())
        .then(data => {
            const flowStatusResponses = data.flowStatusResponses;
            console.log('API Result:', flowStatusResponses);

            // Convert the JSON object to a string for display
            const jsonString = JSON.stringify(flowStatusResponses, null, 2);

            // Display the JSON string in the result div
            document.getElementById('idResutl').innerHTML = `<pre>${jsonString}</pre>`;
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

// Function to make a POST request with a request body
async function postCallFlow() {
    let postBtn = $('#postBtn');
    const url = `https://localhost:8888/api/v1/flow/${selectedX500Name}`;

    // Change the button text to indicate loading
    postBtn.html('Loading...');

    try {
        // Perform the POST request with the provided request body
        const response = await fetch(url, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'Authorization': 'Basic YWRtaW46YWRtaW4=',
                'Content-Type': 'application/json'
            },
            body: `${document.getElementById('requestBody').value}`
        });

        // Parse the response as JSON
        const data = await response.json();

        if (!data.ok) {
            console.log(data.status);

            // Check if the response status is not OK (2xx range)
            if (data.status == "409" || data.status == "400") {
                let flowStatusResponse = "title: " + data.title + "\nStatus: " + data.status;

                // Display additional details for 400 status
                if (data.status == "400") {
                    flowStatusResponse += "\nDetails: \n Cause: " + data.details.cause + "\n Reason: " + data.details.reason;
                }

                document.getElementById('queryResult').innerHTML = `<pre>${flowStatusResponse}</pre>`;
                return;
            }
        }

        let msg = "null";
        let typ = "null";

        // Construct a string with the flow status responses
        let flowStatusResponses = "client request ID: " + data.clientRequestId +
            "\nFlow Result " + data.flowResult +
            "\nFlow Error Message: " + msg +
            "\nflow error type: " + typ +
            "\nFlow ID: " + data.flowId +
            "\nFlow status: " + data.flowStatus +
            "\nHolding identity short hash: " + data.holdingIdentityShortHash +
            "\nTime stamp: " + data.timestamp;

        console.log('API Result:', flowStatusResponses);

        // Display the flow status responses in the result div
        document.getElementById('queryResult').innerHTML = `<pre>${flowStatusResponses}</pre>`;
    } catch (error) {
        console.log('Error:', error);
    } finally {
        // Restore the button text after the operation is complete
        postBtn.html('Post');
    }
}

// Function to display an item on the page
function displayItemOnPage(item) {
    const resultDiv = document.getElementById('queryResult');

    // Create a new element to display the item
    const itemElement = document.createElement('div');
    itemElement.innerHTML = `
        <p>QueryResult: ${item}</p>
    `;

    // Append the new element to the result div
    resultDiv.appendChild(itemElement);
}

// Function to open a specific tab by hiding/showing content
function openTab(tabName) {
    // Hide all tab content
    var tabContents = document.getElementsByClassName("tab-content");
    for (var i = 0; i < tabContents.length; i++) {
        tabContents[i].style.display = "none";
        document.getElementById(tabContents[i].id + "-tab").style.backgroundColor = "rgb(52,152,219)";
    }

    // Show the selected tab content
    var selectedTab = document.getElementById(tabName);
    if (selectedTab) {
        selectedTab.style.display = "block";
        document.getElementById(tabName + "-tab").style.backgroundColor = "rgb(173,216,230)";
    }
}

// Function to display a flow for a specific virtual node
function oneFlow() {
    const url = `https://localhost:8888/api/v1/flow/${selectedX500Name}/${document.getElementById('clientID').value}`;

    // Validate clientID input
    if (document.getElementById('clientID').value == "") {
        alert("Please input a clientId");
        return;
    }

    // Perform a GET request to display a flow
    return fetch(url, {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Authorization': 'Basic YWRtaW46YWRtaW4='
        }
    })
        .then(response => response.json())
        .then(data => {
            let msg = "null";
            let typ = "null";

            // Check if the flow status is "FAILED" and extract error details
            if (data.flowStatus == "FAILED") {
                msg = data.flowError.message;
                typ = data.flowError.type;
            }

            // Construct a string with the flow status responses
            const flowStatusResponses = "client request ID: " + data.clientRequestId +
                "\nFlow Result " + data.flowResult +
                "\nFlow Error Message: " + msg +
                "\nflow error type: " + typ +
                "\nFlow ID: " + data.flowId +
                "\nFlow status: " + data.flowStatus +
                "\nHolding identity short hash: " + data.holdingIdentityShortHash +
                "\nTime stamp: " + data.timestamp;

            console.log('API Result:', flowStatusResponses);

            // Display the flow status responses in the result div
            document.getElementById('idResutl').innerHTML = `<pre>${flowStatusResponses}</pre>`;
        })
        .catch(error => {
            console.error('Error:', error);
        });
}

// Function to determine which flow-related action to execute based on user input
function executeButtonFlow() {
    // Check the value of the dropdown to determine which action to perform
    if (document.getElementById("dropdown").value == "option1") {
        getAllFlowResult();
    } else {
        oneFlow();
    }
}

function queryDropDownChange(){
    if (document.getElementById("dropdown").value == "option1") {
        document.getElementById("clientID").style.display = 'none';
    }else{
        document.getElementById("clientID").style.display = 'block';

    }
}