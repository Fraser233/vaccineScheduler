CREATE TABLE Caregivers (
    Username varchar(255) NOT NULL,
    Salt BINARY(16),
    Hash BINARY(16),
    PRIMARY KEY (Username)
);

CREATE TABLE Availabilities (
    Time date NOT NULL,
    Username varchar(255) REFERENCES Caregivers,
    PRIMARY KEY (Time, Username)
);

CREATE TABLE Vaccines (
    Name varchar(255) NOT NULL,
    Doses int,
    PRIMARY KEY (Name)
);

CREATE TABLE Patients (
    Username varchar(255) NOT NULL,
    Salt BINARY(16),
    Hash BINARY(16),
    Name varchar(255),
    Birthday date,
    Gender varchar(100),
    PRIMARY KEY (Username)
);

CREATE TABLE Schedule (
    ID int NOT NULL,
    P_Username varchar(255) FOREIGN KEY REFERENCES Patients(Username) ON UPDATE CASCADE ON DELETE CASCADE,
    C_Username varchar(255) FOREIGN KEY REFERENCES Caregivers(Username) ON UPDATE CASCADE ON DELETE CASCADE,
    V_Name varchar(255) FOREIGN KEY REFERENCES Vaccines(Name) ON UPDATE CASCADE ON DELETE CASCADE,
    ScheduleDate date,
    Location varchar(255),
    PRIMARY KEY (ID, P_Username, C_Username, V_Name)
);