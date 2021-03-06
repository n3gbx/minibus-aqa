package org.minibus.aqa.test.ui;

import org.minibus.aqa.main.core.helpers.DatesHelper;
import org.minibus.aqa.main.core.helpers.RandomHelper;
import org.minibus.aqa.main.domain.api.models.RouteDTO;
import org.minibus.aqa.main.domain.screens.cities.CitiesScreen;
import org.minibus.aqa.main.domain.screens.schedule.CalendarLayout;
import org.minibus.aqa.main.domain.screens.schedule.ScheduleScreen;
import org.minibus.aqa.test.TestGroup;
import org.testng.annotations.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;


public class ScheduleCalendarTest extends BaseUiTest {

    @Test(groups = {TestGroup.UI},
            description = "When on the Schedule screen then the calendar has 7 days ahead")
    public void testWhenOnScheduleScreenThenCalendarIsWeekly() {
        ScheduleScreen scheduleScreen = new ScheduleScreen();
        scheduleScreen.waitForLoading();

        final int calendarCapacity = DayOfWeek.values().length;
        final List<LocalDate> calendarDates = DatesHelper.getDaysFromNow(calendarCapacity);

        test.assertTrue(scheduleScreen.isOpened(), scheduleScreen.getScreenName() + " screen is opened");
        test.assertEquals(scheduleScreen.getCalendar().getCapacity(), calendarCapacity, "The calendar has desired capacity");
        test.assertEquals(scheduleScreen.getCalendar().getDates(), calendarDates, "The calendar has date values in the correct order");
    }

    @Test(groups = {TestGroup.UI},
            description = "When on the Schedule screen then the calendar has 1 day selected by default")
    public void testWhenOnScheduleScreenThenCalendarHasFirstDaySelected() {
        final int selectedDatePosition = CalendarLayout.DEFAULT_SELECTED_DATE_POSITION;

        ScheduleScreen scheduleScreen = new ScheduleScreen();
        scheduleScreen.waitForLoading();

        final int actualSelectedDatePosition = scheduleScreen.getCalendar().getSelectedDatePosition();

        test.assertTrue(scheduleScreen.isOpened(), scheduleScreen.getScreenName() + " screen is opened");
        test.assertEquals(actualSelectedDatePosition, selectedDatePosition, "Default selected calendar date position is correct");
    }

    @Test(groups = {TestGroup.UI},
            description = "When user changes calendar date then active date changes")
    public void testWhenChangeCalendarDateThenActiveDateChanges() {
        ScheduleScreen scheduleScreen = new ScheduleScreen();
        scheduleScreen.waitForLoading();

        test.assertTrue(scheduleScreen.isOpened(), scheduleScreen.getScreenName() + " screen is opened");

        final int selectedDatePosition = scheduleScreen.getCalendar().getSelectedDatePosition();
        LocalDate selectedDate = scheduleScreen.getCalendar().getSelectedDate();
        List<LocalDate> operationalDates = scheduleScreen.getCalendar().getOperationalDates();

        scheduleScreen.getCalendar().selectDay(RandomHelper.getAny(operationalDates, selectedDate));
        scheduleScreen.waitForContentLoading();

        final int newSelectedDatePosition = scheduleScreen.getCalendar().getSelectedDatePosition();

        test.assertNotEquals(newSelectedDatePosition, selectedDatePosition, "Active calendar date changes");
        test.assertEquals(scheduleScreen.getCalendar().getSelectedDatesCount(), 1, "There is only one currently selected date");
    }

    @Test(groups = {TestGroup.UI}, dataProvider = "dataProvider_RouteWithNonOperationalDays",
            description = "When on the route is chosen then the calendar has correct set of operational days")
    public void testWhenRouteIsChosenThenCalendarHasCorrectSetOfOperationalDays(RouteDTO route) {
        ScheduleScreen scheduleScreen = new ScheduleScreen();
        scheduleScreen.waitForLoading();

        CitiesScreen departureCitiesScreen = scheduleScreen.openDepartureCitiesScreen();
        departureCitiesScreen.waitForLoading();
        departureCitiesScreen.selectCity(route.getFrom().getName());
        scheduleScreen.waitForContentLoading();

        CitiesScreen arrivalCitiesScreen = scheduleScreen.openArrivalCitiesScreen();
        arrivalCitiesScreen.waitForLoading();
        arrivalCitiesScreen.selectCity(route.getTo().getName());
        scheduleScreen.waitForContentLoading();

        List<LocalDate> actualOperationalDates = scheduleScreen.getCalendar().getOperationalDates();
        List<LocalDate> expectedOperationalDates = DatesHelper.fromDayOfWeekValuesToDates(route.getOpDays());

        test.assertEquals(actualOperationalDates, expectedOperationalDates, "Calendar displays correct set of operational days");
    }

    @Test(groups = {TestGroup.UI},
            description = "When user taps on active calendar date then the date remains checked")
    public void testWhenTapOnActiveCalendarDateThenDateRemainsChecked() {
        ScheduleScreen scheduleScreen = new ScheduleScreen();
        scheduleScreen.waitForLoading();

        LocalDate initiallySelectedDate = scheduleScreen.getCalendar().getSelectedDate();
        List<LocalDate> operationalDates = scheduleScreen.getCalendar().getOperationalDates();
        initiallySelectedDate = RandomHelper.getAny(operationalDates, initiallySelectedDate);

        scheduleScreen.getCalendar().selectDay(initiallySelectedDate);
        scheduleScreen.waitForContentLoading();

        final int selectedDatePosition = scheduleScreen.getCalendar().getSelectedDatePosition();

        scheduleScreen.getCalendar().selectDay(initiallySelectedDate);

        test.assertTrue(scheduleScreen.getCalendar().isDaySelected(initiallySelectedDate),
                initiallySelectedDate + " calendar date is checked");
        test.assertEquals(scheduleScreen.getCalendar().getSelectedDatePosition(), selectedDatePosition,
                "Checked calendar date remains on the same position");
    }

    @Test(groups = {TestGroup.UI}, dataProvider = "dataProvider_RouteWithNonOperationalDays",
            description = "When schedule route has non operational day then corresponding calendar date is not clickable")
    public void testWhenRouteHasNonOperationalDayThenCalendarDateIsNotClickable(RouteDTO route) {
        ScheduleScreen scheduleScreen = new ScheduleScreen();
        scheduleScreen.waitForLoading();

        CitiesScreen departureCitiesScreen = scheduleScreen.openDepartureCitiesScreen();
        departureCitiesScreen.waitForLoading();
        departureCitiesScreen.selectCity(route.getFrom().getName());
        scheduleScreen.waitForContentLoading();

        CitiesScreen arrivalCitiesScreen = scheduleScreen.openArrivalCitiesScreen();
        arrivalCitiesScreen.waitForLoading();
        arrivalCitiesScreen.selectCity(route.getTo().getName());
        scheduleScreen.waitForContentLoading();

        LocalDate selectedDate = scheduleScreen.getCalendar().getSelectedDate();
        final DayOfWeek nonOperationalDayOfWeek = RandomHelper.getAny(DatesHelper.extractMissingDaysOfWeek(route.getOpDays()), selectedDate.getDayOfWeek());
        final LocalDate nonOperationalDate = DatesHelper.fromDayOfWeekToDate(nonOperationalDayOfWeek);

        final int selectedDatePosition = scheduleScreen.getCalendar().getSelectedDatePosition();

        scheduleScreen.getCalendar().selectDay(nonOperationalDate);

        test.assertEquals(scheduleScreen.getCalendar().getSelectedDatePosition(), selectedDatePosition,
                "Calendar date doesn't change after selecting non operational date");
    }
}
