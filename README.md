# Multi-User Chat Application

This is a multi-user chat application built using Java. It allows multiple users to connect and communicate with each other in real-time. The project uses Maven for dependency management and build automation.

## Features

*   **User Registration & Login:** Allows users to create accounts and log in.
*   **Real-time Messaging:** Instantaneous sending and receiving of messages.
*   **Multi-User Chat Rooms:** Support for group chats or different chat channels.
*   **Private Messaging:** Ability to send direct messages to other users.
*   **User List:** Display of currently online or available users.
*   **Message History:** Option to view past messages (if implemented).

*Note: This section lists common features for such an application. It can be updated with more specific details once the codebase is fully examined.*

## Technologies Used

*   **Java:** Core programming language (Version 11 as specified in `pom.xml`).
*   **Maven:** Dependency management and build automation.
*   **Hibernate:** Object-Relational Mapping (ORM) framework for database interaction.
*   **MySQL:** Relational database used for storing application data.
*   **Servlet API:** (Implicitly, as the `pom.xml` packaging is `war` and it includes `maven-war-plugin`, suggesting a web application deployment, though specific servlet dependencies aren't listed, it's a common setup).

## Setup and Installation

To get the application up and running, follow these steps:

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd <repository-directory>
    ```
2.  **Prerequisites:**
    *   **Java Development Kit (JDK):** Ensure you have JDK version 11 or higher installed.
    *   **Apache Maven:** Ensure Maven is installed and configured in your system PATH.
    *   **MySQL Server:** Make sure you have a MySQL server instance running.
3.  **Database Configuration:**
    *   Create a database schema/user for the application in MySQL.
    *   Update the database connection details (URL, username, password) in the Hibernate configuration file (usually found in `src/main/resources/hibernate.cfg.xml` or configured within a persistence unit in `persistence.xml`). *(Note: The exact file might need to be located and specified if different).*
4.  **Build the project:**
    Open a terminal in the project's root directory (`Chat Application/`) and run:
    ```bash
    mvn clean install
    ```
    This will compile the source code, run any tests, and package the application (typically as a WAR file in the `target/` directory).
5.  **Run the application:**
    *   The method to run the application depends on how it's packaged and designed (e.g., deploying the WAR file to a Servlet container like Apache Tomcat, or if it has an embedded server).
    *   *(Placeholder: Specific instructions on how to run the server and client components need to be added here once the application's entry points are identified.)*

## Usage

Once the application is installed and running:

1.  **Launch the Client:** (Details on how to launch the client application need to be specified here. This might involve running a JAR file or accessing a web interface.)
2.  **Register/Login:** If it's your first time, you might need to register a new user account. Otherwise, log in with your existing credentials.
3.  **Chat:**
    *   You should see a list of available users or chat rooms.
    *   Select a user for a private chat or join a chat room.
    *   Type your messages in the input field and press Enter to send.

*(Note: This is a general guide. Specific usage instructions will depend on the application's UI and workflow.)*

## Contributing

Contributions are welcome! If you'd like to contribute to this project, please follow these steps:

1.  **Fork the repository.**
2.  **Create a new branch** for your feature or bug fix:
    ```bash
    git checkout -b feature/your-feature-name
    ```
    or
    ```bash
    git checkout -b bugfix/issue-tracker-id
    ```
3.  **Make your changes** and commit them with clear, descriptive messages.
4.  **Push your changes** to your forked repository:
    ```bash
    git push origin feature/your-feature-name
    ```
5.  **Create a Pull Request (PR)** against the main repository's `main` or `master` branch.
    *   Clearly describe the changes you've made and why.
    *   Reference any relevant issues.

Please ensure your code adheres to any existing coding standards and includes tests where applicable.

## License

*(Placeholder: This project does not currently have a specified license. It is recommended to add an open-source license file (e.g., MIT, Apache 2.0) to the repository to clarify how others can use and contribute to the project.)*

For example, if you choose the MIT License, you would add a `LICENSE.txt` file with the MIT License text and then update this section to:

"This project is licensed under the MIT License - see the [LICENSE.txt](LICENSE.txt) file for details."
