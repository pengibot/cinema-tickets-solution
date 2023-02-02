package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.*;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * TicketServiceImpl.java
 * Represents a ticket service that is responsible for implementing the TicketService Interface
 */
public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    // A Logger that allows messages to be output for debugging and information purposes
    private final Logger logger = Logger.getLogger(TicketServiceImpl.class.getName());

    // Price in £ of an Adult ticket. Not good to hard code, better to get from service.
    private final int ADULT_TICKET_PRICE = 20;

    // Price in £ of a Child ticket. Not good to hard code, better to get from service.
    private final int CHILD_TICKET_PRICE = 10;

    // A reference to a Ticket Payment Service implementation
    private final TicketPaymentService ticketPaymentService;

    // A reference to a Seat Reservation Service implementation
    private final SeatReservationService seatReservationService;

    /**
     * Assuming TicketServiceImpl is created by passing in TicketPaymentService and SeatReservationService.
     * I have used this constructor to create a set of Junit/Mockito tests in TicketPaymentServiceImplTest.java.
     * Constructor allows different implementations of TicketPaymentService and SeatReservationService
     * to be passed in. This can be done through Dependency Injection or manual means.
     *
     * @param ticketPaymentService A service that allows payments to be made
     * @param seatReservationService A service that allows seats to be reserved
     */
    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        // Could use dependency injection here to pass in specific service
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Validates Business rules reserving seats and making payment
     *
     * @param accountId a value indicating the customer's ID
     * @param ticketTypeRequests a list of tickets indicating the type and number of tickets
     * @throws InvalidPurchaseException if any of the business rules are broken
     */
    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateInputParameters(accountId, ticketTypeRequests);
        validateNumberOfTicketsDoesNotExceedMaximum(ticketTypeRequests);
        validateAdultToInfantRatioCorrect(ticketTypeRequests);
        validateChildOrInfantWithAdult(ticketTypeRequests);

        // Getting seats first in case payment is taken and there are no seats left
        // Would have to consider putting this in a Unit of Work pattern to rollback
        // if concurrency issues occur.
        seatReservationService.reserveSeat(accountId, getNumberOfSeats(ticketTypeRequests));
        ticketPaymentService.makePayment(accountId, calculateTotalCostOfTickets(ticketTypeRequests));
    }

    /**
     * Validates the input parameters are correct
     * <p>
     * Validates that account id is greater than zero
     * Validates that ticketTypeRequests is not null and there are tickets being sent in
     * Checks that none of those ticket requests are empty or have negative values
     *
     * @param accountId a value indicating the customer's ID
     * @param ticketTypeRequests a list of tickets indicating the type and number of tickets
     * @throws InvalidPurchaseException if any of the parameters appear to be invalid
     */
    private void validateInputParameters(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        if (accountId <= 0 || ticketTypeRequests == null || ticketTypeRequests.length == 0 || Arrays.stream(ticketTypeRequests).anyMatch(x -> x.getNoOfTickets() <= 0)) {
            logger.info("Invalid input parameters");
            throw new InvalidPurchaseException();
        } else {
            logger.info("Input parameters successfully validated");
        }
    }

    /**
     * Validates whether the number of tickets exceeds the maximum allowed to be purchased/booked
     *
     * @param ticketTypeRequests a list of tickets indicating the type and number of tickets
     * @throws InvalidPurchaseException if the number of tickets exceeds the maximum
     */
    private void validateNumberOfTicketsDoesNotExceedMaximum(TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        int numberOfTickets = Arrays.stream(ticketTypeRequests).mapToInt(TicketTypeRequest::getNoOfTickets).sum();
        int maximumTickets = 20; // Not good to hard code, better to get from service.
        if (numberOfTickets > maximumTickets || numberOfTickets <= 0) {
            logger.info("Tried to purchase " + numberOfTickets + " tickets, where minimum is 1 and maximum is " + maximumTickets);
            throw new InvalidPurchaseException();
        } else {
            logger.info("Successfully validated that tickets are meeting or below maximum");
        }
    }

    /**
     * Calculates and returns the number of seats that need reserving
     * <p>
     * Business Rule 3 - The ticket purchaser declares how many and what type of tickets they want to buy.
     * Business Rule 6 - Infants do not pay for a ticket and are not allocated a seat. They will be sitting on an Adult's lap.
     *
     * @param ticketTypeRequests a list of tickets indicating the type and number of tickets
     * @return the number of seats for booking
     */
    private Integer getNumberOfSeats(TicketTypeRequest... ticketTypeRequests) {
        int seats =  Arrays.stream(ticketTypeRequests)
                .filter(ticket -> ticket.getTicketType() != TicketTypeRequest.Type.INFANT)
                .mapToInt(TicketTypeRequest::getNoOfTickets)
                .sum();

        logger.info(seats + " seats allocated for booking");

        return seats;
    }

    /**
     * Calculates and returns the total cost of all the tickets bought
     * <p>
     * Business Rule 6 - Infants do not pay for a ticket and are not allocated a seat. They will be sitting on an Adult's lap.
     * Business Rule 2 - The ticket prices are based on the type of ticket (see table below).
     * <p>
     * |   Ticket Type    |     Price   |
     * | ---------------- | ----------- |
     * |    INFANT        |    £0       |
     * |    CHILD         |    £10      |
     * |    ADULT         |    £20      |
     *
     * @param ticketTypeRequests a list of tickets indicating the type and number of tickets
     * @return the total cost of all the tickets purchased
     * @throws InvalidPurchaseException if there is a ticket type it does not recognise
     */
    private Integer calculateTotalCostOfTickets(TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        int cost =  Arrays.stream(ticketTypeRequests).mapToInt(ticket -> switch (ticket.getTicketType()) {
            case ADULT -> ticket.getNoOfTickets() * ADULT_TICKET_PRICE;
            case CHILD -> ticket.getNoOfTickets() * CHILD_TICKET_PRICE;
            case INFANT -> 0; // Not good to hard code, better to get from service.
        }).sum();
        logger.info("Cost calculated as £" + cost);
        return cost;
    }

    /**
     * Validates there is at least 1 Adult present for every infant. Infants are not allocated a seat, so for every
     * infant travelling there must be an Adult that it can sit on their lap.
     * <p>
     * Business Rule 6 - Infants do not pay for a ticket and are not allocated a seat.
     * They will be sitting on an Adult's lap.
     *
     * @param ticketTypeRequests a list of tickets indicating the type and number of tickets
     * @throws InvalidPurchaseException if there is a more infants than adults
     */
    private void validateAdultToInfantRatioCorrect(TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        int adultTickets = Arrays.stream(ticketTypeRequests).filter(ticket -> ticket.getTicketType() == TicketTypeRequest.Type.ADULT).mapToInt(TicketTypeRequest::getNoOfTickets).sum();
        int infantTickets = Arrays.stream(ticketTypeRequests).filter(ticket -> ticket.getTicketType() == TicketTypeRequest.Type.INFANT).mapToInt(TicketTypeRequest::getNoOfTickets).sum();

        if(infantTickets > adultTickets) {
            logger.info("Too few adult tickets purchased in relation to infant tickets");
            throw new InvalidPurchaseException();
        } else {
            logger.info("Successfully validated there is at least one adult ticket for each infant travelling");
        }
    }

    /**
     * Business Rule 7 - Child and Infant tickets cannot be purchased without purchasing an Adult ticket.
     *
     * @param ticketTypeRequests a list of tickets indicating the type and number of tickets
     * @throws InvalidPurchaseException if there is no adult tickets being purchased
     */
    private void validateChildOrInfantWithAdult(TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

       boolean valid = Arrays.stream(ticketTypeRequests)
                .anyMatch(ticket -> ticket.getTicketType() == TicketTypeRequest.Type.ADULT && ticket.getNoOfTickets() > 0);

       if(valid) {
           logger.info("Successfully validated there is at least one adult ticket for booking");
       } else {
           logger.info("There was not an adult ticket type being purchased");
           throw new InvalidPurchaseException();
       }
    }
}
