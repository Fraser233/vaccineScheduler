package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) throws SQLException {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");  //TODO: implement create_patient (Part 1) √ √ √ √ √
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");  // TODO: implement login_patient (Part 1) √ √ √ √ √
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");  // TODO: implement search_caregiver_schedule (Part 2) √ √ √ √ √
        System.out.println("> reserve <date> <vaccine>");  // TODO: implement reserve (Part 2) √ √ √ √ √
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");  // TODO: implement cancel (extra credit)
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");  // TODO: implement show_appointments (Part 2)
        System.out.println("> logout");  // TODO: implement logout (Part 2)
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_Patient <username> <password>

        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);

        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            currentPatient.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";

        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occur when checking username.");
            e.printStackTrace();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println(" *** Account created successfully *** ");
        } catch (SQLException e) {
            System.out.println("Create failed");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in! Please log out first.");
            return;
        }
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        if (patient == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Patient logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("Already logged-in! Please log out first.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when logging in");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Please try again!");
        } else {
            System.out.println("Caregiver logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // search_caregiver_schedule <date>

        // Both patients and caregivers can perform this operation.
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in first.");
            return;
        }

        // Output the username for the caregivers that are available for the date,
        //      along with the number of available doses left for each vaccine.

        String scheduleDate = tokens[1];

        // Filter out caregivers for given date in the database.
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getCaregivers = "SELECT Username FROM [dbo].[Availabilities] WHERE Time = ?;";

        try {
            PreparedStatement statement = con.prepareStatement(getCaregivers);
            statement.setString(1, scheduleDate);
            ResultSet resultSet = statement.executeQuery();

            System.out.println("Caregivers who are available for this date: ");
            while (resultSet.next())
                System.out.println(resultSet.getString("Username"));
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for caregivers.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        // Get vaccine information from the database.
        cm = new ConnectionManager();
        con = cm.createConnection();

        String getVaccines = "SELECT * FROM [dbo].[Vaccines];";

        try {
            PreparedStatement statement = con.prepareStatement(getVaccines);
            ResultSet resultSet = statement.executeQuery();

            System.out.println("");
            System.out.println("Vaccine available now: ");
            System.out.println("--");

            while (resultSet.next()) {
                System.out.println("Name: " + resultSet.getString("Name"));
                System.out.println("Doses: " + resultSet.getString("Doses"));
                System.out.println("--");
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for vaccines.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void reserve(String[] tokens) throws SQLException {
        // reserve <date> <vaccine>

        // Only patients can perform this operation to reserve an appointment.
        if (currentPatient == null) {
            System.out.println("Please log in patient account.");
            return;
        }

        // check the token length.
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        // Local variables:
        String resDate = tokens[1];
        String resVaccine = tokens[2];
        java.sql.Date date = null;
        String randCaregiver = "";
        int currID = 0;

        // Randomly assigned a caregiver for the reservation on that date.
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getRandCaregiver = "SELECT TOP 1 * FROM Availabilities WHERE Time = ? ORDER BY NEWID();";

        try {
            PreparedStatement statement = con.prepareStatement(getRandCaregiver);
            statement.setString(1, resDate);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                date = resultSet.getDate("Time");
                randCaregiver = resultSet.getString("Username");
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when assigning caregiver.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        // Output the assigned caregiver and the appointment ID for the reservation.

        // Get the last ID to set the current one.
        String getLastID = "SELECT MAX(ID) max FROM Schedule;";

        cm = new ConnectionManager();
        con = cm.createConnection();

        try {
            PreparedStatement statement = con.prepareStatement(getLastID);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.getRow() == 0)
                currID = 1;
            else currID = resultSet.getInt("max") + 1;
        } catch (SQLException e) {
            System.out.println("Error occurred when assigning schedule ID.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        // Get the patient username.
        String P_username = currentPatient.getUsername();

        cm = new ConnectionManager();
        con = cm.createConnection();

        String assignSchedule = "INSERT INTO Schedule VALUES (?, ?, ?, ?, ?, ?);";

        try {
            PreparedStatement statement = con.prepareStatement(assignSchedule);
            statement.setInt(1, currID);
            statement.setString(2, P_username);
            statement.setString(3, randCaregiver);
            statement.setString(4, resVaccine);
            statement.setDate(5, date);
            statement.setString(6, "loc01");
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading schedule.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        System.out.println("Your appointment ID is " + currID);
        System.out.println("Your caregiver is " + randCaregiver);
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static void cancel(String[] tokens) {
        // cancel <appointment_id>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null || currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }

        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        // local variables
        String appointID = tokens[1];

        // connect to the database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAppointment = "DELETE FROM Schedule WHERE ID= ?;";

        try {
            PreparedStatement statement = con.prepareStatement(getAppointment);
            statement.setString(1, appointID);
        } catch (SQLException e) {
            System.out.println("Error occurred when canceling schedule.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // For caregivers, appointment ID, vaccine name, date, and patient name.
        // For patients, appointment ID, vaccine name, date, and caregiver name.

        // check if already log in
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please log in first.");
            return;
        }

        // check who is logging in, caregiver or patient
        boolean isPatient = currentCaregiver == null;

        // local variables
        String yourName = isPatient? currentPatient.getUsername() : currentCaregiver.getUsername();
        List<Integer> appointID = new ArrayList<>();
        List<String> vaccineName = new ArrayList<>();
        List<Date> appointDate = new ArrayList<>();
        List<String> patientOrCaregiverName = new ArrayList<>();

        // connect to the database
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAppointment = "";

        if (isPatient)
            getAppointment = "SELECT * FROM [dbo].[Schedule] WHERE P_Username = ?;";
        else getAppointment = "SELECT * FROM [dbo].[Schedule] WHERE C_Username = ?;";

        try {
            PreparedStatement statement = con.prepareStatement(getAppointment);
            statement.setString(1, yourName);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                appointID.add(resultSet.getInt("ID"));
                vaccineName.add(resultSet.getString("V_Name"));
                appointDate.add(resultSet.getDate("ScheduleDate"));

                if (isPatient)
                    patientOrCaregiverName.add(resultSet.getString("C_Username"));
                else patientOrCaregiverName.add(resultSet.getString("P_Username"));
            }
        } catch (SQLException e) {
            System.out.println("Error occurred when finding schedule.");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }

        // print out the result
        System.out.println("Account: " + yourName);
        System.out.println("-----------------");

        for (int i = 0; i < appointID.size(); i++) {
            if (isPatient) {
                System.out.println("Your appointment ID: " + appointID.get(i));
                System.out.println("Your vaccine name: " + vaccineName.get(i));
                System.out.println("Appointment date: " + appointDate.get(i));
                System.out.println("Your caregiver: " + patientOrCaregiverName.get(i));
            } else {
                System.out.println("Appointment ID: " + appointID.get(i));
                System.out.println("Vaccine name: " + vaccineName.get(i));
                System.out.println("Appointment date: " + appointDate.get(i));
                System.out.println("Your patient: " + patientOrCaregiverName.get(i));
            }
            System.out.println("-----------------");
        }
    }

    private static void logout(String[] tokens) {
        // check if someone has login
        if (currentCaregiver == null || currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }

        // logout
        currentCaregiver = null;
        currentPatient = null;
    }
}