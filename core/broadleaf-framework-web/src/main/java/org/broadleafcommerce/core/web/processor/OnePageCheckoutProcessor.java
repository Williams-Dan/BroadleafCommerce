/*
 * #%L
 * BroadleafCommerce Framework Web
 * %%
 * Copyright (C) 2009 - 2016 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */

package org.broadleafcommerce.core.web.processor;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.broadleafcommerce.common.money.Money;
import org.broadleafcommerce.common.payment.PaymentGatewayType;
import org.broadleafcommerce.common.payment.PaymentType;
import org.broadleafcommerce.common.vendor.service.exception.FulfillmentPriceException;
import org.broadleafcommerce.common.web.BroadleafRequestContext;
import org.broadleafcommerce.common.web.payment.controller.PaymentGatewayAbstractController;
import org.broadleafcommerce.core.order.domain.FulfillmentGroup;
import org.broadleafcommerce.core.order.domain.FulfillmentOption;
import org.broadleafcommerce.core.order.domain.NullOrderImpl;
import org.broadleafcommerce.core.order.domain.Order;
import org.broadleafcommerce.core.order.service.FulfillmentGroupService;
import org.broadleafcommerce.core.order.service.FulfillmentOptionService;
import org.broadleafcommerce.core.payment.domain.OrderPayment;
import org.broadleafcommerce.core.payment.service.OrderToPaymentRequestDTOService;
import org.broadleafcommerce.core.pricing.service.FulfillmentPricingService;
import org.broadleafcommerce.core.pricing.service.fulfillment.provider.FulfillmentEstimationResponse;
import org.broadleafcommerce.core.web.checkout.model.BillingInfoForm;
import org.broadleafcommerce.core.web.checkout.model.OrderInfoForm;
import org.broadleafcommerce.core.web.checkout.model.ShippingInfoForm;
import org.broadleafcommerce.core.web.checkout.section.CheckoutSectionDTO;
import org.broadleafcommerce.core.web.checkout.section.CheckoutSectionStateType;
import org.broadleafcommerce.core.web.checkout.section.CheckoutSectionViewType;
import org.broadleafcommerce.core.web.order.CartState;
import org.broadleafcommerce.presentation.condition.TemplatingExistCondition;
import org.broadleafcommerce.presentation.dialect.AbstractBroadleafVariableModifierProcessor;
import org.broadleafcommerce.presentation.model.BroadleafTemplateContext;
import org.broadleafcommerce.profile.core.domain.CustomerAddress;
import org.broadleafcommerce.profile.core.domain.Address;
import org.broadleafcommerce.profile.core.service.AddressService;
import org.broadleafcommerce.profile.core.service.CountryService;
import org.broadleafcommerce.profile.core.service.CustomerAddressService;
import org.broadleafcommerce.profile.core.service.StateService;
import org.broadleafcommerce.profile.web.core.CustomerState;
import org.joda.time.DateTime;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * This is a Thymeleaf Processor that aids in rendering a dynamic One Page Checkout screen.
 * These sections are driven by the state of the order and whether or not certain integrated payment
 * modules are enabled (e.g. PayPal Express Review Page etc...).
 * 
 * <p>
 * For example, if there are no shippable fulfillment
 * groups on the order, then the shipping section should not be shown. Or, if the user is redirected
 * back from a Third Party Payment gateway to complete the order (e.g. PayPal Express Checkout), then
 * the billing section will not be shown.
 *
 * @author Elbert Bautista (elbertbautista)
 */
@Component("blOnePageCheckoutProcessor")
@Conditional(TemplatingExistCondition.class)
public class OnePageCheckoutProcessor extends AbstractBroadleafVariableModifierProcessor {

    @Resource(name = "blStateService")
    protected StateService stateService;

    @Resource(name = "blCountryService")
    protected CountryService countryService;

    @Resource(name = "blAddressService")
    protected AddressService addressService;

    @Resource(name = "blCustomerAddressService")
    protected CustomerAddressService customerAddressService;

    @Resource(name = "blFulfillmentGroupService")
    protected FulfillmentGroupService fulfillmentGroupService;

    @Resource(name = "blFulfillmentOptionService")
    protected FulfillmentOptionService fulfillmentOptionService;

    @Resource(name = "blFulfillmentPricingService")
    protected FulfillmentPricingService fulfillmentPricingService;

    @Resource(name = "blOrderToPaymentRequestDTOService")
    protected OrderToPaymentRequestDTOService orderToPaymentRequestDTOService;

    @Override
    public String getName() {
        return "one_page_checkout";
    }
    
    @Override
    public int getPrecedence() {
        return 100;
    }
    
    @Override
    public boolean useGlobalScope() {
        return false;
    }

    @Override
    public Map<String, Object> populateModelVariables(String tagName, Map<String, String> tagAttributes, BroadleafTemplateContext context) {
        //Pre-populate the command objects
        OrderInfoForm orderInfoForm = (OrderInfoForm) context.parseExpression(tagAttributes.get("orderInfoForm"));

        ShippingInfoForm shippingInfoForm = (ShippingInfoForm) context.parseExpression(tagAttributes.get("shippingInfoForm"));

        BillingInfoForm billingInfoForm = (BillingInfoForm) context.parseExpression(tagAttributes.get("billingInfoForm"));

        String orderInfoHelpMessage = (String) context.parseExpression(tagAttributes.get("orderInfoHelpMessage"));

        String billingInfoHelpMessage = (String) context.parseExpression(tagAttributes.get("billingInfoHelpMessage"));

        String shippingInfoHelpMessage = (String) context.parseExpression(tagAttributes.get("shippingInfoHelpMessage"));

        prepopulateCheckoutForms(CartState.getCart(), orderInfoForm, shippingInfoForm, billingInfoForm);

        //Add PaymentRequestDTO to the model in the case of errors or other cases
        Map<String, Object> newModelVars = new HashMap<>();
        Order cart = CartState.getCart();
        if (cart != null && !(cart instanceof NullOrderImpl)) {
            newModelVars.put("paymentRequestDTO", orderToPaymentRequestDTOService.translateOrder(cart));
        }

        //Initialize Fulfillment Group Vars
        int numShippableFulfillmentGroups = calculateNumShippableFulfillmentGroups();
        newModelVars.put("numShippableFulfillmentGroups", numShippableFulfillmentGroups);
        populateFulfillmentOptionsAndEstimationOnModel(newModelVars);

        //Initialize View States
        newModelVars.put("orderInfoHelpMessage", orderInfoHelpMessage);
        newModelVars.put("billingInfoHelpMessage", billingInfoHelpMessage);
        newModelVars.put("shippingInfoHelpMessage", shippingInfoHelpMessage);

        populateSectionViewStates(newModelVars);

        //Helpful lists to populate dropdowns on a checkout page
        newModelVars.put("states", stateService.findStates());
        newModelVars.put("countries", countryService.findCountries());
        newModelVars.put("expirationMonths", populateExpirationMonths());
        newModelVars.put("expirationYears", populateExpirationYears());

        //Populate any Payment Processing Errors
        populateProcessingError(newModelVars);

        return newModelVars;
    }

    /**
     * The Checkout page for Heat Clinic will have the shipping information pre-populated
     * with an address if the fulfillment group has an address and fulfillment option
     * associated with it. It also assumes that if there is only one order payment of type
     * credit card on the order, then the billing address will be pre-populated with that payment.
    */
    protected void prepopulateCheckoutForms(Order cart,
                                            OrderInfoForm orderInfoForm,
                                            ShippingInfoForm shippingForm,
                                            BillingInfoForm billingForm) {

        if (orderInfoForm != null) {
            orderInfoForm.setEmailAddress(cart.getEmailAddress());
        }

        FulfillmentGroup firstShippableFulfillmentGroup = fulfillmentGroupService.getFirstShippableFulfillmentGroup(cart);
        if (firstShippableFulfillmentGroup != null) {
            //if the cart has already has fulfillment information
            if (firstShippableFulfillmentGroup.getAddress() != null) {
                shippingForm.setAddress(firstShippableFulfillmentGroup.getAddress());
            } else {
                //check for a default address for the customer
                CustomerAddress defaultAddress = customerAddressService.findDefaultCustomerAddress(CustomerState.getCustomer().getId());
                if (defaultAddress != null) {
                    Address address = addressService.readAddressById(defaultAddress.getAddressExternalId());
                    shippingForm.setAddress(address);
                    shippingForm.setAddressName(defaultAddress.getAddressName());
                }
            }

            FulfillmentOption fulfillmentOption = firstShippableFulfillmentGroup.getFulfillmentOption();
            if (fulfillmentOption != null) {
                shippingForm.setFulfillmentOption(fulfillmentOption);
                shippingForm.setFulfillmentOptionId(fulfillmentOption.getId());
            }
        }

        if (cart.getPayments() != null) {
            for (OrderPayment payment : cart.getPayments()) {
                if (PaymentType.CREDIT_CARD.equals(payment.getType())) {
                    if (payment.getBillingAddress() != null) {
                        billingForm.setAddress(payment.getBillingAddress());
                    }
                }
            }
        }
    }

    protected int calculateNumShippableFulfillmentGroups() {
        int numShippableFulfillmentGroups = 0;
        List<FulfillmentGroup> fulfillmentGroups = CartState.getCart().getFulfillmentGroups();
        if (fulfillmentGroups != null) {
            for (FulfillmentGroup fulfillmentGroup : fulfillmentGroups) {
                if (fulfillmentGroupService.isShippable(fulfillmentGroup.getType())) {
                    numShippableFulfillmentGroups++;
                }
            }
        }
        return numShippableFulfillmentGroups;
    }

    /**
     * This method is responsible for populating any Payment Procecessing Errors that may have been put
     * as a Redirect Attribute when attempting to checkout.
     *
     * @param localVars
     */
    protected void populateProcessingError(Map<String, Object> localVars) {
        BroadleafRequestContext blcContext = BroadleafRequestContext.getBroadleafRequestContext();
        HttpServletRequest request = blcContext.getRequest();
        String processorError = request.getParameter(PaymentGatewayAbstractController.PAYMENT_PROCESSING_ERROR);
        localVars.put(PaymentGatewayAbstractController.PAYMENT_PROCESSING_ERROR, processorError);
    }

    /**
     * This method is responsible of populating the variables necessary to draw the checkout page.
     * This logic is highly dependent on your layout. If your layout does not follow the same flow
     * as the HeatClinic demo, you will need to override with your own custom layout implementation
     *
     * @param localVars
     */
    protected void populateSectionViewStates(Map<String, Object> localVars) {
        boolean orderInfoPopulated = hasPopulatedOrderInfo(CartState.getCart());
        boolean billingPopulated = hasPopulatedBillingAddress(CartState.getCart());
        boolean shippingPopulated = hasPopulatedShippingAddress(CartState.getCart());

        localVars.put("orderInfoPopulated", orderInfoPopulated);
        localVars.put("billingPopulated", billingPopulated);
        localVars.put("shippingPopulated", shippingPopulated);

        //Logic to show/hide sections based on state of the order
        // show all sections including header unless specifically hidden
        // (e.g. hide shipping if no shippable items in order or hide billing section if the order payment doesn't need
        // an address i.e. PayPal Express)
        boolean showBillingInfoSection = true;
        boolean showShippingInfoSection = true;
        boolean showAllPaymentMethods = true;
        boolean showPaymentMethodSection = true;

        int numShippableFulfillmentGroups = calculateNumShippableFulfillmentGroups();
        if (numShippableFulfillmentGroups == 0) {
            showShippingInfoSection = false;
        }

        boolean orderContainsThirdPartyPayment = false;
        boolean orderContainsUnconfirmedCreditCard = false;
        OrderPayment unconfirmedCC = null;
        if (CartState.getCart().getPayments() != null) {
            for (OrderPayment payment : CartState.getCart().getPayments()) {
                if (payment.isActive() && PaymentType.THIRD_PARTY_ACCOUNT.equals(payment.getType())) {
                    orderContainsThirdPartyPayment = true;
                }
                if (payment.isActive() && (PaymentType.CREDIT_CARD.equals(payment.getType()) && !PaymentGatewayType.TEMPORARY.equals(payment.getGatewayType()))) {
                    orderContainsUnconfirmedCreditCard = true;
                    unconfirmedCC = payment;
                }
            }
        }

        //Toggle the Payment Info Section based on what payments were applied to the order
        //(e.g. Third Party Account (i.e. PayPal Express) or Gift Cards/Customer Credit)
        Money orderTotalAfterAppliedPayments = CartState.getCart().getTotalAfterAppliedPayments();
        if (orderContainsThirdPartyPayment || orderContainsUnconfirmedCreditCard) {
            showBillingInfoSection = false;
            showAllPaymentMethods = false;
        } else if (orderTotalAfterAppliedPayments != null && orderTotalAfterAppliedPayments.isZero()) {
            //If all the applied payments (e.g. gift cards) cover the entire amount
            //we don't need to show all payment method options.
            showAllPaymentMethods = false;
        }

        localVars.put("showBillingInfoSection", showBillingInfoSection);
        localVars.put("showAllPaymentMethods", showAllPaymentMethods);
        localVars.put("showPaymentMethodSection", showPaymentMethodSection);
        localVars.put("orderContainsThirdPartyPayment", orderContainsThirdPartyPayment);
        localVars.put("orderContainsUnconfirmedCreditCard", orderContainsUnconfirmedCreditCard);
        localVars.put("unconfirmedCC", unconfirmedCC);

        //The Sections are all initialized to INACTIVE view
        List<CheckoutSectionDTO> drawnSections = new LinkedList<>();
        CheckoutSectionDTO orderInfoSection = new CheckoutSectionDTO(CheckoutSectionViewType.ORDER_INFO, orderInfoPopulated);
        CheckoutSectionDTO billingInfoSection = new CheckoutSectionDTO(CheckoutSectionViewType.BILLING_INFO, billingPopulated);
        CheckoutSectionDTO shippingInfoSection = new CheckoutSectionDTO(CheckoutSectionViewType.SHIPPING_INFO, shippingPopulated);
        CheckoutSectionDTO paymentInfoSection = new CheckoutSectionDTO(CheckoutSectionViewType.PAYMENT_INFO, false);

        String orderInfoHelpMessage = (String) localVars.get("orderInfoHelpMessage");
        String billingInfoHelpMessage = (String) localVars.get("billingInfoHelpMessage");
        String shippingInfoHelpMessage = (String) localVars.get("shippingInfoHelpMessage");

        //Add the Order Info Section
        drawnSections.add(orderInfoSection);

        //Add the Billing Section
        if (showBillingInfoSection) {
            billingInfoSection.setHelpMessage(orderInfoHelpMessage);
            drawnSections.add(billingInfoSection);
        }

        //Add the Shipping Section
        if (showShippingInfoSection) {

            if (showBillingInfoSection) {
                shippingInfoSection.setHelpMessage(billingInfoHelpMessage);
            } else {
                shippingInfoSection.setHelpMessage(orderInfoHelpMessage);
            }

            drawnSections.add(shippingInfoSection);
        }

        //Add the Payment Section
        if (showShippingInfoSection) {
            paymentInfoSection.setHelpMessage(shippingInfoHelpMessage);
        } else if (showBillingInfoSection) {
            paymentInfoSection.setHelpMessage(billingInfoHelpMessage);
        } else {
            paymentInfoSection.setHelpMessage(orderInfoHelpMessage);
        }

        drawnSections.add(paymentInfoSection);

        //Logic to toggle state between form view, saved view, and inactive view
        //This is dependent on the layout of your checkout form. Override this if layout is different.

        //initialize first view to always be a FORM view
        CheckoutSectionDTO firstSection = drawnSections.get(0);
        firstSection.setState(CheckoutSectionStateType.FORM);
        //iterate through all the drawn sections and set their state based on the state of the other sections.

        for (ListIterator<CheckoutSectionDTO> itr = drawnSections.listIterator(); itr.hasNext();) {
            CheckoutSectionDTO previousSection = null;
            if (itr.hasPrevious()) {
                previousSection = drawnSections.get(itr.previousIndex());
            }
            CheckoutSectionDTO section = itr.next();

            //if the previous section is populated, set this section to a Form View
            if (previousSection != null && previousSection.isPopulated()) {
                section.setState(CheckoutSectionStateType.FORM);
            }
            //If this sections is populated then set this section to the Saved View
            if (section.isPopulated()) {
                section.setState(CheckoutSectionStateType.SAVED);
            }

            //Custom Logic to handle a state where there may have been an error on the Payment Gateway
            //and the customer is booted back to the checkout page and will have to re-enter their billing address
            //and payment information as there may have been an error on either. Since, to handle all gateways with the same layout
            //we are breaking the Billing Address Form from the Payment Info Form, to serve a better UX, we will have hide the payment info as
            //the customer will need to re-enter their billing address to try again.
            //{@see DefaultPaymentGatewayCheckoutService where payments are invalidated on an unsuccessful transaction}
            if (CheckoutSectionViewType.PAYMENT_INFO.equals(section.getView())) {
                if (showBillingInfoSection && !billingPopulated) {
                    section.setState(CheckoutSectionStateType.INACTIVE);
                    section.setHelpMessage(billingInfoHelpMessage);
                }
            }

            //Finally, if the edit button is explicitly clicked, set the section to Form View
            BroadleafRequestContext blcContext = BroadleafRequestContext.getBroadleafRequestContext();
            HttpServletRequest request = blcContext.getRequest();
            boolean editOrderInfo = BooleanUtils.toBoolean(request.getParameter("edit-order-info"));
            boolean editBillingInfo = BooleanUtils.toBoolean(request.getParameter("edit-billing"));
            boolean editShippingInfo = BooleanUtils.toBoolean(request.getParameter("edit-shipping"));

            if (CheckoutSectionViewType.ORDER_INFO.equals(section.getView()) && editOrderInfo) {
                section.setState(CheckoutSectionStateType.FORM);
            } else if (CheckoutSectionViewType.BILLING_INFO.equals(section.getView()) && editBillingInfo) {
                section.setState(CheckoutSectionStateType.FORM);
            } else if (CheckoutSectionViewType.SHIPPING_INFO.equals(section.getView()) && editShippingInfo) {
                section.setState(CheckoutSectionStateType.FORM);
            }
        }

        localVars.put("checkoutSectionDTOs", drawnSections);

    }

    /**
     * A helper method to retrieve all fulfillment options for the cart and estimate the cost of applying
     * fulfillment options on the first shippable fulfillment group.
     *
     */
    protected void populateFulfillmentOptionsAndEstimationOnModel(Map<String, Object> localVars) {
        List<FulfillmentOption> fulfillmentOptions = fulfillmentOptionService.readAllFulfillmentOptions();
        Order cart = CartState.getCart();

        if (!(cart instanceof NullOrderImpl) && cart.getFulfillmentGroups().size() > 0 && hasPopulatedShippingAddress(cart)) {
            Set<FulfillmentOption> options = new HashSet<>();
            options.addAll(fulfillmentOptions);
            FulfillmentEstimationResponse estimateResponse = null;
            try {
                estimateResponse = fulfillmentPricingService.estimateCostForFulfillmentGroup(fulfillmentGroupService.getFirstShippableFulfillmentGroup(cart), options);
            } catch (FulfillmentPriceException e) {

            }

            localVars.put("estimateResponse", estimateResponse);
        }

        localVars.put("fulfillmentOptions", fulfillmentOptions);
    }

    /**
     * A helper method used to determine the validity of order info
     *
     * @param cart
     * @return boolean indicating whether or not the order has valid info
     */
    protected boolean hasPopulatedOrderInfo(Order cart) {
        return StringUtils.isNotBlank(cart.getEmailAddress());
    }

    /**
     * A helper method used to determine the validity of the payments on the Order
     *
     * @param cart
     * @return boolean indicating whether or not the CREDIT_CARD order payment on the order has an address
     */
    protected boolean hasPopulatedBillingAddress(Order cart) {
        if (cart.getPayments() == null) {
            return false;
        }

        for (OrderPayment payment : cart.getPayments()) {
            if (payment.isActive() && PaymentType.CREDIT_CARD.equals(payment.getType()) && payment.getBillingAddress() != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * A helper method used to determine the validity of the fulfillment groups
     *
     * @param cart
     * @return boolean indicating whether or not the fulfillment groups on the cart have addresses.
     */
    protected boolean hasPopulatedShippingAddress(Order cart) {
        if (cart.getFulfillmentGroups() == null) {
            return false;
        }
        boolean populatedShippingAddress = false;
        for (FulfillmentGroup fulfillmentGroup : cart.getFulfillmentGroups()) {
            if (fulfillmentGroupService.isShippable(fulfillmentGroup.getType())) {
                if (fulfillmentGroup.getAddress() != null && fulfillmentGroup.getFulfillmentOption() != null) {
                    populatedShippingAddress = true;
                }
            }
        }
        return populatedShippingAddress;
    }

    /**
     * A helper method used to construct a list of Credit Card Expiration Months
     * Useful for expiration dropdown menus.
     * Will use locale to determine language if a locale is available.
     *
     * @return List containing expiration months of the form "01 - January"
     */
    protected List<String> populateExpirationMonths() {
        DateFormatSymbols dateFormatter;
        if (BroadleafRequestContext.hasLocale()) {
            Locale locale = BroadleafRequestContext.getBroadleafRequestContext().getJavaLocale();
            dateFormatter = new DateFormatSymbols(locale);
        } else {
            dateFormatter = new DateFormatSymbols();
        }
        List<String> expirationMonths = new ArrayList<>();
        NumberFormat formatter = new DecimalFormat("00");
        String[] months = dateFormatter.getMonths();
        for (int i = 1; i < months.length; i++) {
            expirationMonths.add(formatter.format(i) + " - " + months[i - 1]);
        }
        return expirationMonths;
    }

    /**
     * A helper method used to construct a list of Credit Card Expiration Years
     * Useful for expiration dropdown menus.
     *
     * @return List of the next ten years starting with the current year.
     */
    protected List<String> populateExpirationYears() {
        List<String> expirationYears = new ArrayList<>();
        DateTime dateTime = new DateTime();
        for (int i = 0; i < 10; i++) {
            expirationYears.add(dateTime.plusYears(i).getYear() + "");
        }
        return expirationYears;
    }

}
