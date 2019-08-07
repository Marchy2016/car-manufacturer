package com.testing.project.java_ee.control;


import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.logging.Logger;

@Startup
@Singleton
public class UserRadiusQueryService {

    private static final Logger LOGGER = Logger.getLogger(UserRadiusQueryService.class.getName());

    @Resource
    private TimerService timerService;

    @Inject
    private ApplicationPropertyRepository applicationPropertyRepository;

    @Inject
    private RadacctRepository radacctRepository;

    @Inject
    private RegionIPAddressRepository regionIPAddressRepository;

    @Inject
    private AccountServiceRepository accountServiceRepository;

    @Inject
    private IPManagementClient ipManagementClient;

    @Inject
    private AttributeRepository attributeRepository;

    @Inject
    private AccountStatusRepository accountStatusRepository;

    @Inject
    private WorkflowClient workflowClient;

    @Resource
    ManagedScheduledExecutorService mses;

    private String timerName;
    private int initialDurationInMinutes;
    private int intervalDurationInMinutes;

    // private int subnet;
    private static final MediaType[] ACCEPT_MEDIA_TYPES = {/*MediaType.APPLICATION_JSON_TYPE,*/ MediaType.APPLICATION_XML_TYPE};


    @PostConstruct
    private void init() {

        if (!applicationPropertyRepository.exists(ApplicationProperties.LDAP_ATTRIBUTE_UPDATE_TIMER_ENABLED_KEY)) {
            ApplicationProperty property = new ApplicationProperty();
            property.setId(ApplicationProperties.LDAP_ATTRIBUTE_UPDATE_TIMER_ENABLED_KEY);
            property.setValue(String.valueOf(ApplicationProperties.LDAP_ATTRIBUTE_UPDATE_TIMER_ENABLED));

            applicationPropertyRepository.save(property);
        }

        if (!applicationPropertyRepository.exists(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INTERVAL_DURATION_ATTRIBUTE_KEY)) {
            ApplicationProperty property = new ApplicationProperty();
            property.setId(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INTERVAL_DURATION_ATTRIBUTE_KEY);
            property.setValue(String.valueOf(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INTERVAL_DURATION));

            applicationPropertyRepository.save(property);
        }

        if (!applicationPropertyRepository.exists(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INITIAL_DURATION_IN_MINUTES_ATTRIBUTE_KEY)) {
            ApplicationProperty property = new ApplicationProperty();
            property.setId(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INITIAL_DURATION_IN_MINUTES_ATTRIBUTE_KEY);
            property.setValue(String.valueOf(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INITIAL_DURATION_IN_MINUTES));

            applicationPropertyRepository.save(property);
        }

        if (!applicationPropertyRepository.exists(ApplicationProperties.LDAP_ATTRIBUTE_UPDATE_TIMER_NAME_ATTRIBUTE_KEY)) {
            ApplicationProperty property = new ApplicationProperty();
            property.setId(ApplicationProperties.LDAP_ATTRIBUTE_UPDATE_TIMER_NAME_ATTRIBUTE_KEY);
            property.setValue(ApplicationProperties.LDAP_ATTRIBUTE_UPDATE_TIMER_NAME);
            //Save
            applicationPropertyRepository.save(property);
        }

        timerName = applicationPropertyRepository.get(ApplicationProperties.LDAP_ATTRIBUTE_UPDATE_TIMER_NAME_ATTRIBUTE_KEY);
        initialDurationInMinutes = applicationPropertyRepository.getAsInt(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INITIAL_DURATION_IN_MINUTES_ATTRIBUTE_KEY);
        intervalDurationInMinutes = applicationPropertyRepository.getAsInt(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INTERVAL_DURATION_ATTRIBUTE_KEY);
        startRadiusConnectionService(timerName, initialDurationInMinutes, intervalDurationInMinutes);

    }

    //Check for pending status in account service and use the user email to query the ip
    @Timeout
    private void updateLDAPAttributes() {

        if (applicationPropertyRepository.getAsBoolean(ApplicationProperties.LDAP_ATTRIBUTE_UPDATE_TIMER_ENABLED_KEY)) {
            if (intervalDurationInMinutes != applicationPropertyRepository.getAsInt(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INTERVAL_DURATION_ATTRIBUTE_KEY)) {
                updateUpdateLDAPAttributesTimer(intervalDurationInMinutes = applicationPropertyRepository.getAsInt(ApplicationProperties.LDAP_ATTRIBUTES_UPDATE_TIMER_INTERVAL_DURATION_ATTRIBUTE_KEY));
            } else {
                LOGGER.info("Starting to update ldap attributes for bb static ip: " + LocalDateTime.now());
                updateAccountAttributes();
            }
        }
    }

    private void updateAccountAttributes() {

        AccountService accountService = getPendingAccounts();
        if (accountService != null) {
            for (AccountService accountService2 : getAccountProduct(accountService).getServices()) {
                if (accountService2 instanceof AccountAccessService) {
                    String username = ((AccountAccessService) accountService2).getUserName();
                    getAccountAttributeAndUpdateLDAP(accountService2, username);
                }
            }
        }

    }

    private void getAccountAttributeAndUpdateLDAP(AccountService accountService, String username) {

        for (ProductAttribute productAttribute : accountService.getAccountProduct().getProduct().getProductAttributes()) {
            int subnet = Integer.parseInt(productAttribute.getAttributeValue());
            setLDAPAttributes(username, accountService, subnet);
        }

    }

    private AccountProduct getAccountProduct(AccountService accountService) {

        AccountProduct accountProduct = null;
        try {
            for (AccountProduct accotProduct : accountService.getAccountProduct().getAccount().getProducts()) {
                if (!accountProduct.isVas()) {
                    accountProduct = accotProduct;
                }
            }
        } catch (Exception ex) {
            LOGGER.info("Error retrieving account product " + ex.getMessage());
        }
        return accountProduct;
    }


    private AccountService getPendingAccounts() {

        AccountService accountService = null;
        try {
            Collection<AccountService> accountServices = accountServiceRepository.findAllByStatusId(AccountStatuses.ACCOUNT_STATUS_PENDING);
            for (AccountService accontService : accountServices) {
                accountService = accontService;
            }
        } catch (Exception ex) {
            LOGGER.info("Error retrieving pending accounts" + ex.getMessage());
        }
        return accountService;
    }

    private void setLDAPAttributes(String username, AccountService accountService, int subnet) {

        if (username != null) {

            try {
                String ipAddress = radacctRepository.findIPAddressByUserName(username);
                String region = regionIPAddressRepository.findRegionByIPAddress(ipAddress);
                if (region != null) {
                    ReservationIPAddressDTO reservationIPAddressDTO = ipManagementClient.reserveIPAddress(accountService.getAccountProduct().getAccount().getId(), subnet, region);
                    if (reservationIPAddressDTO != null) {

                        accountService.setStatus(accountStatusRepository.find(AccountStatuses.ACCOUNT_STATUS_ACTIVE));
                        AccountIpAddress accountIpAddress = new AccountIpAddress();
                        accountIpAddress.setIpAddress(reservationIPAddressDTO.getIp().getIpAddress());
                        accountIpAddress.setAccountService(accountService);
                        accountIpAddress.setReserved(true);

                        AccountServiceAttribute productSubnetServiceAttribute = new AccountServiceAttribute();
                        AccountServiceAttribute radiusRouteServiceAttribute = new AccountServiceAttribute();
                        AccountServiceAttribute radiusIPAddressServiceAttribute = new AccountServiceAttribute();

                        //Setting LDAP attributes
                        productSubnetServiceAttribute.setAttribute(attributeRepository.find(ATTRIBUTE_ID_PRODUCT_SUBNET_SIZE));
                        radiusRouteServiceAttribute.setAttribute(attributeRepository.find(ATTRIBUTE_ID_RADIUS_FRAMED_ROUTE));
                        radiusIPAddressServiceAttribute.setAttribute(attributeRepository.find(ATTRIBUTE_ID_RADIUS_FRAMED_IP_ADDRESS));

                        productSubnetServiceAttribute.setAttributeValue(String.valueOf(reservationIPAddressDTO.getIp().getIpSubnet().getSize()));
                        radiusRouteServiceAttribute.setAttributeValue(reservationIPAddressDTO.getIp().getRegion().getRouterRegion());
                        radiusIPAddressServiceAttribute.setAttributeValue(reservationIPAddressDTO.getIp().getIpAddress());

                        productSubnetServiceAttribute.setAccountService(accountService);
                        accountService.getServiceAttributes().add(productSubnetServiceAttribute);

                        radiusRouteServiceAttribute.setAccountService(accountService);
                        accountService.getServiceAttributes().add(radiusRouteServiceAttribute);

                        radiusIPAddressServiceAttribute.setAccountService(accountService);
                        accountService.getServiceAttributes().add(radiusIPAddressServiceAttribute);

                        accountServiceRepository.save(accountService);
                        workflowClient.updateLDAPAttributes(accountService.getId());
                    }
                }
            } catch (Exception ex) {
                LOGGER.info("Error setting up LDAP attributes " + ex.getMessage());
            }

        }

    }


    private void startRadiusConnectionService(final String timerName, final int initialDurationInMinutes, final int intervalDurationInMinutes) {
        TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(Boolean.FALSE);
        timerConfig.setInfo(timerName);
        //    mses.scheduleAtFixedRate(this::doSomething, 60, 10, TimeUnit.SECONDS);

        long initialDurationMillis = initialDurationInMinutes * 60 * 100;
        long intervalDurationMillis = intervalDurationInMinutes * 60 * 1000;
        timerService.createIntervalTimer(initialDurationMillis, intervalDurationMillis, timerConfig);
        LOGGER.info("Radius Connection service started, interval = " + intervalDurationInMinutes + " minute(s)");
    }

    private void updateUpdateLDAPAttributesTimer(int newIntervalDurationInMinutes) {
        LOGGER.info("Updating cleanup service timer from " + this.intervalDurationInMinutes + "minute(s) to " + newIntervalDurationInMinutes + " minutes");
        for (Timer timer : timerService.getAllTimers()) {
            if (timer.getInfo().equals(timerName)) {
                timer.cancel();
            }
        }
        startRadiusConnectionService(timerName, initialDurationInMinutes, newIntervalDurationInMinutes);
    }

}
