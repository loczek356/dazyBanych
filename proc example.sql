CREATE PROCEDURE dbo.usp_EnrollInCourse
    @Email VARCHAR(255),
    @CourseII INT,
    @Message NVARCHAR(255) OUTPUT
AS
BEGIN
    SET NOCOUNT ON;

    -- Deklarowanie pomocniczych zmiennych
    DECLARE
        @UserID INT,
        @IsActiveUser BIT,
        @IsActiveCourse BIT,
        @ExistingEnrolls INT,
        @BasePrice MONEY,
        @DiscountVal DECIMAL(10, 4),
        @DiscountType VARCHAR(50),
        @TotalCost MONEY,
        @AvailableSlots INT;

    BEGIN TRANSACTION;

    BEGIN TRY

        -- Sprawdzanie istnienia użytkowanika wg email
        SELECT
            @UserID = user_id,
            @IsActiveUser = is_active
        FROM users_user
        WHERE email = @Email;

        IF @UserID IS NULL
        BEGIN
            -- Użytkownik nie istnieje
            INSERT INTO users_user (
                email,
                first_name,
                last_name,
                is_active,
                current_balance
            )
            VALUES (
                @Email,
                'Unknown', -- first_name
                'Unknown', -- last_name
                1,         -- nowy użytkownik jest aktywny
                0          -- początkowe saldo
            );

            SET @UserID = SCOPE_IDENTITY(); -- UserID będzie automatycznie dopisane wg IDENTITY(1,1)
            SET @IsActiveUser = 1;
        END
        ELSE IF @IsActiveUser = 0
        BEGIN
            -- Użytkownik istnieje, ale jest nieaktywny
            SET @Message = N'User with given username is inactive';

            ROLLBACK;
            RETURN;
        END;

        -- Sprawdzenie, czy kurs istnieje i jest aktywny
        SELECT
            @IsActiveCourse = is_active,
            @BasePrice = base_price
        FROM course
        WHERE course_id = @CourseID;

        IF @IsActiveCourse IS NULL
        BEGIN
            SET @Message =
                N'Course with ID = ' +
                CAST(@CourseID AS NVARCHAR(10)) +
                N' does not exist.';

            ROLLBACK;
            RETURN;
        END
        ELSE IF @IsActiveCourse = 0
        BEGIN
            SET @Message = N'Course is inactive.';

            ROLLBACK;
            RETURN;
        END;

        -- Sprawdzenie, ile jest wolnych miejsc w kursie
        DECLARE @TotalCapacity INT;
        DECLARE @AlreadyEnrolled INT;

        SELECT
            @TotalCapacity = c.planned_groups_amout * SUM(g.max_group_capacity)
        FROM course AS c
        JOIN [group] AS g
            ON c.course_id = g.course_id
        WHERE c.course_id = @CourseID
        GROUP BY c.planned_groups_amount;

        -- Podliczenie już istniejących zapisów
        SELECT
            @AlreadyEnrolled = COUNT(*)
        FROM course_enrollment
        WHERE course_id = @CourseID
            AND is_dropped = 0;

        SET @AvailableSlots = @TotalCapacity - @AlreadyEnrolled;

        IF @AvailableSlots < 1
        BEGIN
            SET @Message = N'No slots left in the course.';

            ROLLBACK;
            RETURN;
        END;

        -- Obliczanie wartości DISCOUNT i TOTAL_COST na podstawie liczby poprzednich zapisów
        SELECT
            @ExistingEnrolls = COUNT(*)
        FROM course_enrollment
        WHERE user_id = @UserID
            AND is_dropped = 0;

        IF @ExistingEnrolls = 0
        BEGIN
            -- Pierwszy zakup - bezwarunkowy rabat 100zł
            SET @DiscountVal = 100.00;
            SET @DiscountType = N'bezwarunkowy';

            SET @TotalCost = @BasePrice - @DiscountVal;

            IF @TotalCost < 0
                SET @TotalCost = 0;
        END
        ELSE IF @ExistingEnrolls = 1
        BEGIN
            -- Drugi zakup - stały rabat 5%
            SET @DiscountVal = 0.05;
            SET @DiscountType = N'staly';

            SET @TotalCost = @BasePrice * (1.0 - @DiscountVal);
        END
        ELSE
        BEGIN
            -- >2 zakup - lojalnościowy rabat 0.05 + (n * 0.01)
            SET @DiscountVal = 0.05 + (@ExistingEnrolls * 0.01);
            SET @DiscountType = N'lojalnosciowy';

            SET @TotalCost = @BasePrice * (1.0 - @DiscountVal);

            IF @TotalCost < 0
                SET @TotalCost = 0;
        END;

        -- Wstawianie nowego zapisu do course_enrollment
        INSERT INTO course_enrollment (
            user_id,
            course_id,
            enrollment_date,
            total_cost,
            discount_value,
            is_completed,
            is_dropped
        )
        VALUES (
            @UserID,
            @CourseID,
            GETDATE(),
            @TotalCost,
            @DiscountType,
            @DiscountVal,
            0, -- is_completed
            0  -- is_dropped
        );

        SET @Message =
            N'Course enrollment successful. Cost: ' +
            CAST(@TotalCost AS NVARCHAR(20)) +
            N' zł.';

        COMMIT;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK;

        SET @Message = ERROR_MESSAGE();
    END CATCH;
END;
