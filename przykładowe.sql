-- 1. Tworzenie bazy danych
CREATE DATABASE DB__Kowalski_123456;
GO

USE DB__Kowalski_123456;
GO

-- 2. Tworzenie tabeli ZAMOWIENIA
CREATE TABLE ZAMOWIENIA_Kowalski_123456 (
    Id_zamowienia INT IDENTITY(1,1) PRIMARY KEY,
    Nazwa_rosliny VARCHAR(100) NOT NULL,
    Cena_sztuki DECIMAL(10, 2) NOT NULL,
    Ilosc INT NOT NULL,
    Cena_calosciowa DECIMAL(10, 2) NOT NULL
);
GO