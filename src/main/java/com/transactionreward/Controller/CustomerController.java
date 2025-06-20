package com.transactionreward.Controller;

import com.transactionreward.Entity.Customer;
import com.transactionreward.Entity.Rewards;
import com.transactionreward.Exception.TransactionFailed;
import com.transactionreward.Service.CustomerServiceImpl;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customer")
public class CustomerController {
    // Autowired Service Layer Implementation.
    @Autowired
    CustomerServiceImpl customerService;

    // Logger for the class to log appropriate messages.
    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    /*
    Controller endpoint for adding reword
    Request Body Contains the user request body for adding reword.
    */
    @PostMapping(value = "/createCustomer")
    public ResponseEntity<Customer> addCustomer(@Valid @RequestBody Customer request ,@RequestParam String customerName ){
        Rewards rewords = null;
        try {
            logger.info("Adding customer in progress: {}", request);
            Customer SavedCustomer= customerService.createCustomer(request);
            logger.info("Completed request to add Customer: {}", request);
            return new ResponseEntity<>(SavedCustomer, HttpStatus.OK);
        }
        catch (Exception e){
            logger.warn("Adding customer in progress: {}", request);
            throw new TransactionFailed("User Creation Failed");

        }

    }



}
