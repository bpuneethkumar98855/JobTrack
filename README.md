# JobTrack – Job Application Tracker

JobTrack is a full-stack web application that helps job seekers organize, track, and manage their job applications in one place. It provides application management, status tracking, dashboard statistics, and AI-powered extraction of job details.

## Features

* **Dashboard:** View and organize job application information.
* **Application Management:** Add, view, edit, and delete job applications.
* **Status Tracking:** Track application progress through statuses such as Applied, Shortlisted, Interview, Offer, Rejected, and Withdrawn.
* **Status History:** Record application status changes and view the timeline on the application details page.
* **Search and Filter:** Find applications using relevant search terms and filters.
* **AI-Powered Job Detail Extraction:** Use the Groq API to extract useful information from job descriptions.
* **Database Integration:** Store application information in MySQL.
* **Responsive Interface:** Use the application through a web-based interface.

## Technologies Used

**Backend**

* Java
* Spring Boot
* Spring Data JPA
* Hibernate
* Maven
* REST APIs

**Frontend**

* HTML
* CSS
* JavaScript

**Database and Integration**

* MySQL
* Groq API

## Project Structure

```text
Job_application_tracker/
├── index.html
├── applications.html
├── add-application.html
├── edit-application.html
├── application-details.html
├── css/
│   └── style.css
├── js/
│   └── app.js
└── jobtrack/
    ├── src/
    │   ├── main/
    │   └── test/
    ├── pom.xml
    └── mvnw
```

## Prerequisites

Before running the project, install:

* Java 21
* MySQL
* Git
* A modern web browser

## Setup and Installation

### 1. Clone the repository

```bash
git clone https://github.com/bpuneethkumar98855/JobTrack.git
cd JobTrack
```

### 2. Create the MySQL database

Open MySQL Workbench and run:

```sql
CREATE DATABASE jobtrack;
```

The application expects the required tables to exist in the database. Configure the schema according to the application's entity definitions and database setup documentation.

### 3. Configure environment variables

Set the database connection details and Groq API key using environment variables or a local `.env` file.

Example `.env` file inside the `jobtrack` directory:

```properties
DB_URL=jdbc:mysql://localhost:3306/jobtrack
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
GROQ_API_KEY=your_groq_api_key
```

Replace the example values with your local configuration.

**Security:** Never commit your `.env` file, API keys, or database passwords to GitHub.

### 4. Start the Spring Boot backend

Open a terminal in the `jobtrack` directory.

On Windows PowerShell:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/jobtrack"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
.\mvnw.cmd spring-boot:run
```

Configure `GROQ_API_KEY` as well if you want to use AI-powered extraction.

The backend runs at:

```text
http://localhost:8080
```

### 5. Open the frontend

Open `index.html` using VS Code Live Server or another local web server.

The frontend communicates with the backend API at:

```text
http://localhost:8080/api/applications
```

## API Overview

The backend exposes REST endpoints for managing applications, updating application statuses, retrieving status history, and extracting details from job descriptions.

The main application API base path is:

```text
/api/applications
```

## Learning Outcomes

This project demonstrates practical experience with:

* Building REST APIs using Spring Boot
* Java and object-oriented programming
* Database integration using MySQL and JPA
* CRUD operations
* Frontend-to-backend communication
* Configuration using environment variables
* Unit and integration testing
* Integrating an external AI API

## Author

**Puneeth Kumar Bodapati**

GitHub: [bpuneethkumar98855](https://github.com/bpuneethkumar98855)

Portfolio: [View Portfolio](https://portfolio-six-amber-47.vercel.app/)

---

This project was developed as a hands-on learning project to strengthen full-stack development skills with Java and Spring Boot.
