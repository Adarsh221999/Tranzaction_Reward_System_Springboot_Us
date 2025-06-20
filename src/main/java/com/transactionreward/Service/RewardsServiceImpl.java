package com.transactionreward.Service;

import com.transactionreward.Entity.Rewards;
import com.transactionreward.Exception.*;
import com.transactionreward.Models.RewardSummeryByCustomer;
import com.transactionreward.Repository.CustomerRepo;
import com.transactionreward.Repository.RewardsRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RewardsServiceImpl implements RewardOperations {

    @Autowired
    private RewardsRepo repo;

    @Autowired
    private CustomerRepo customerRepo;

    private static final Logger loggerRewardservice = LoggerFactory.getLogger(RewardsServiceImpl.class);

    //Constructor for the class
    public RewardsServiceImpl(RewardsRepo RewardsRepo) {
    }

    /*
    Method for the calculation for the list of Rewards by CustomerID.
     */
    @Override
    public List<Rewards> findByCustomerId(Long CustomerId) {
        return repo.findByCustomerId(CustomerId);
    }

    /*
    Method to add reword in table with given customer id in body and transactionId autogenerated.
     */
    public Rewards addRewardPoints(Rewards Rewards) {
        Rewards savedReword=null;
        try {
            loggerRewardservice.info("Adding reword at service level "+ Rewards);
            Rewards.setRewardPoints(calculateRewardsPoints(Rewards.getTransactionAmount()));
            Rewards.setDate(LocalDate.now());
            boolean isUserExist = customerRepo.existsById(Rewards.getCustomer().getId());
            try {
                if (isUserExist) {
                    savedReword = repo.save(Rewards);
                }
                else
                {
                    throw new CustomerAlreadyExists("Customer Not Exist By Id"+ Rewards.getCustomer().getId());
                }
            }
            catch (CustomerAlreadyExists e){
                 throw new CustomerAlreadyExists("Customer Not Exist By Id"+ Rewards.getCustomer().getId());
            }

            loggerRewardservice.info("Adding reword at service level Completed"+ Rewards);

        } catch (Exception e) {
            loggerRewardservice.warn("Exception While Adding reword at service level "+ Rewards);
            throw new TransactionFailed("Adding Rewards Transaction Failed Either Customer Not Exist or any other error.  ");
        }
        return savedReword;
    }

    /*
    Method for the calculation for the reword points by transactionId/RewordId.
     */
    @Override
    public Rewards getRewardPoints(Integer rewordId) {
        Rewards getRewardsById=null;
        try {
            loggerRewardservice.info("Getting Rewards by Id {}", rewordId);
            getRewardsById= repo.getById(rewordId);
            loggerRewardservice.info("Getting Rewards by Id {}Completed", rewordId);

            return  getRewardsById;
        }
        catch (Exception e ){
            loggerRewardservice.info("Exception While Getting Rewards by Id "+ rewordId);
            throw new RewordTransactionNotFound("No Reword Transaction Found");
        }
    }

    /*
     Method for the calculation for the reword Summery points by Customer ID.
    */
    @Override
    public RewardSummeryByCustomer findRewardSummeryMonthlyByCustomerId(Long customerId, LocalDate StartDate, LocalDate EndDate) {
        RewardSummeryByCustomer summery = new RewardSummeryByCustomer();
        List<Rewards> allRewards=new ArrayList<Rewards>();
        List<Map<String,Object>> transactionRecords=new ArrayList<>();

        try
        {
            loggerRewardservice.info("Getting Rewards by CustomerId By Month And Total  "+ customerId);

          try {
               allRewards = repo.findByCustomerId(customerId);
          }
          catch (CustomerNotFoundException e){
              throw e;
          }
            if (allRewards.isEmpty())
            {
                throw new  CustomerNotFoundException("No Customer with the id "+customerId);
            }
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
            long totalPoints= allRewards.stream().mapToLong(Rewards::getRewardPoints).sum();
            summery.setTotalSumOfAllRewards(totalPoints);
            List<Map<String,Object>> rewordsSummery= new ArrayList<Map<String,Object>>();
            Map<String, Integer>RewardsByMonth = allRewards.stream().collect(Collectors.groupingBy(Rewords->(Rewords.getDate().format(formatter)),Collectors.summingInt(points->points.getRewardPoints().intValue())));

            for (Map.Entry<String,Integer> record : RewardsByMonth.entrySet()){
                Map<String,Object> rewardEntry = new HashMap<>();
                rewardEntry.put("Points:",record.getValue());
                rewardEntry.put("Month:",record.getKey());
                rewordsSummery.add(rewardEntry);
            }
            for (Rewards reward : allRewards){
                  Map<String,Object> transactions=new HashMap<>();
                  transactions.put("Amount",reward.getTransactionAmount());
                  transactions.put("TransactionId",reward.getTransactionId());
                  transactionRecords.add(transactions);
            }
            summery.setRewardPoints(rewordsSummery);
            summery.setTransactionList(transactionRecords);
            summery.setCustomerName(allRewards.getLast().getCustomerName());
            loggerRewardservice.info("Getting Rewards by CustomerId By Month And Total  "+ customerId+"Completed");
            return summery;

        } catch (Exception e) {
            loggerRewardservice.warn("Exception while Getting Rewards by CustomerId By Month And Total  "+ customerId);
            throw new RuntimeException(e);
        }

    }

    /*
    Method for the calculation for the reword points by transaction amount.
     */
    private static Long calculateRewardsPoints(Double amount) {
        long total_Reword_Points = 0;
        try {
            loggerRewardservice.info("Getting Rewards Calculated By Amount "+ amount + "Started");
            long transaction_amount = Math.round(amount);

            if (transaction_amount > 100) {
                total_Reword_Points += (transaction_amount - 100) * 2 + (50);
            } else if (transaction_amount > 50 && transaction_amount < 100) {
                total_Reword_Points = (transaction_amount - 50);
            } else {
               return total_Reword_Points;
            }

            loggerRewardservice.info("Getting Rewards Calculated By Amount "+ amount + "Completed");

        }
        catch(Exception e){
            loggerRewardservice.warn("Exception caught Rewards Calculated By Amount "+ amount + "Started");
            throw e;
        }

        return total_Reword_Points;
    }

    /*
    Below method gives summery of Rewards for Last 3 Month date range using method Reward summeryMonthly;
     */
    public List<RewardSummeryByCustomer> getRewardsummeryForLastThreeMonth(LocalDate StartDate, LocalDate EndDate) {
       List<RewardSummeryByCustomer> all_Customer_Rewardsummery_LastThreeMonths = new ArrayList<>();;
        try {
            if (StartDate.isAfter(EndDate)){
                throw new IllegalArgumentException("Start Date Should Not Be Greater Than EndDate");
            }
            List<Rewards> RewordOfLastThreeMonths = repo.findByDateBetween(StartDate,EndDate);
            for (Rewards Rewards : RewordOfLastThreeMonths) {
                RewardSummeryByCustomer RewardsummeryMonthlyByCustomerId = findRewardSummeryMonthlyByCustomerId(Rewards.getCustomer().getId(),StartDate,EndDate);
                if(all_Customer_Rewardsummery_LastThreeMonths.contains(RewardsummeryMonthlyByCustomerId)){
                    continue;
                }
                all_Customer_Rewardsummery_LastThreeMonths.add(RewardsummeryMonthlyByCustomerId);
            }
            return all_Customer_Rewardsummery_LastThreeMonths;
        }
        catch(NullPointerException e ){
            throw new NullPointerException("Getting summery got into Null Pointer Exception");
        }
        catch (Exception e) {
           throw new FailedToGetRewardSummeryForLastThreeMonth("Unable to generate summery of Rewards for last three months");
        }
    }
}