package uk.gov.dwp.uc.pairtest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.mockito.Mockito.*;

public class TicketPaymentServiceImplTest {

    private SeatReservationService seatReservationServiceMock;
    private TicketPaymentService ticketPaymentServiceMock;
    private TicketService ticketService;

    @Before
    public void setup() {
        seatReservationServiceMock = mock(SeatReservationService.class);
        ticketPaymentServiceMock = mock(TicketPaymentService.class);
        ticketService = new TicketServiceImpl(ticketPaymentServiceMock, seatReservationServiceMock);
    }

    @After
    public void teardown() {
        seatReservationServiceMock = null;
        ticketPaymentServiceMock = null;
    }

    @Test(expected=InvalidPurchaseException.class)
    public void invalidInputsThrowException_invalidID() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets(0L, ttr1);
    }

    @Test(expected=InvalidPurchaseException.class)
    public void invalidInputsThrowException_noRequests() {
        ticketService.purchaseTickets(1L);
    }

    @Test
    public void purchaseSingleAdultTicket() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets(1L, ttr1);
        verify(seatReservationServiceMock).reserveSeat(1L, 1);
        verify(ticketPaymentServiceMock).makePayment(1L, 20);
    }

    @Test
    public void purchaseMultipleAdultTickets() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10);
        ticketService.purchaseTickets(1L, ttr1);
        verify(seatReservationServiceMock).reserveSeat(1L, 10);
        verify(ticketPaymentServiceMock).makePayment(1L, 200);
    }

    @Test
    public void purchaseMaximumAdultTickets() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
        ticketService.purchaseTickets(1L, ttr1);
        verify(seatReservationServiceMock).reserveSeat(1L, 20);
        verify(ticketPaymentServiceMock).makePayment(1L, 400);
    }

    @Test(expected=InvalidPurchaseException.class)
    public void purchaseAboveMaximumAdultTickets() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 21);
        ticketService.purchaseTickets(1L, ttr1);
    }

    @Test
    public void purchaseMultipleAdultTicketRequests() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 5);
        TicketTypeRequest ttr2= new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
        verify(seatReservationServiceMock).reserveSeat(1L, 9);
        verify(ticketPaymentServiceMock).makePayment(1L, 180);
    }

    @Test(expected=InvalidPurchaseException.class)
    public void purchaseMultipleAdultTicketRequestsThatExceedMaximum() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 15);
        TicketTypeRequest ttr2= new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 6);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseSingleChildTicketWithNoAdult() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        ticketService.purchaseTickets(1L, ttr1);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseSingleInfantTicketWithNoAdult() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets(1L, ttr1);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseMultipleChildTicketWithNoAdult() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        ticketService.purchaseTickets(1L, ttr1);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseMultipleInfantTicketWithNoAdult() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);
        ticketService.purchaseTickets(1L, ttr1);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseMultipleChildTicketRequestsWithNoAdult() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 4);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseMultipleInfantTicketRequestsWithNoAdult() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 4);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
    }

    @Test
    public void purchaseSingleChildTicketWithSingleAdult() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
        verify(seatReservationServiceMock).reserveSeat(1L, 2);
        verify(ticketPaymentServiceMock).makePayment(1L, 30);
    }

    @Test
    public void purchaseMultipleChildTicketsWithSingleAdult() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 5);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
        verify(seatReservationServiceMock).reserveSeat(1L, 6);
        verify(ticketPaymentServiceMock).makePayment(1L, 70);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseMultipleChildAndAdultTicketsThatExceedsMaximum() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 15);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseMultipleInfantAndAdultTicketsThatExceedsMaximum() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 10);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 15);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseSingleChildTicketWithInfant() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void purchaseTwoInfantTicketsWithOnlySingleAdultTicket() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
    }

    @Test
    public void purchaseInfantTicketWithAdultTicketValidTicketTypes() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
        verify(seatReservationServiceMock).reserveSeat(1L, 1);
        verify(ticketPaymentServiceMock).makePayment(1L, 20);
    }

    @Test
    public void purchaseMultipleValidTicketTypes() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 4);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest ttr3 = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3);
        ticketService.purchaseTickets(1L, ttr1, ttr2, ttr3);
        verify(seatReservationServiceMock).reserveSeat(1L, 6);
        verify(ticketPaymentServiceMock).makePayment(1L, 100);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void ticketRequestWithNoTickets() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        ticketService.purchaseTickets(1L, ttr1);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void ticketRequestWithNegativeNumberOfTickets() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -5);
        ticketService.purchaseTickets(1L, ttr1);
    }

    @Test(expected= InvalidPurchaseException.class)
    public void mixedTicketRequestsWithNegativeNumbers() {
        TicketTypeRequest ttr1 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -5);
        TicketTypeRequest ttr2 = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 7);
        ticketService.purchaseTickets(1L, ttr1, ttr2);
    }
}
