/*
 * #%L
 * BroadleafCommerce Admin Module
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
package com.broadleafcommerce.order.admin.web.rulebuilder.service;

import org.broadleafcommerce.common.presentation.RuleIdentifier;
import org.broadleafcommerce.common.presentation.client.SupportedFieldType;
import org.broadleafcommerce.openadmin.web.rulebuilder.dto.FieldData;
import org.broadleafcommerce.openadmin.web.rulebuilder.service.AbstractRuleBuilderFieldService;
import org.springframework.stereotype.Service;

/**
 * An implementation of a RuleBuilderFieldService
 * that constructs metadata necessary
 * to build the supported fields for a Fulfillment Group entity
 *
 * @author Elbert Bautista (elbertbautista)
 */
@Service("blFulfillmentGroupFieldService")
public class FulfillmentGroupFieldServiceImpl  extends AbstractRuleBuilderFieldService {

    @Override
    public void init() {
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupFirstName")
                .name("address.firstName")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupLastName")
                .name("address.lastName")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupAddresLine1")
                .name("address.addressLine1")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupAddressLine2")
                .name("address.addressLine2")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupCity")
                .name("address.cityLocality")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupState")
                .name("address.stateProvinceRegion")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupPostalCode")
                .name("address.postalCode")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupCountry")
                .name("address.countryCode")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupPrimaryPhone")
                .name("address.phone")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupTotal")
                .name("total")
                .operators("blcOperators_Numeric")
                .options("[]")
                .type(SupportedFieldType.MONEY)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupPrice")
                .name("fulfillmentPrice")
                .operators("blcOperators_Numeric")
                .options("[]")
                .type(SupportedFieldType.MONEY)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupRetailPrice")
                .name("retailFulfillmentPrice")
                .operators("blcOperators_Numeric")
                .options("[]")
                .type(SupportedFieldType.MONEY)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupSalePrice")
                .name("saleFulfillmentPrice")
                .operators("blcOperators_Numeric")
                .options("[]")
                .type(SupportedFieldType.MONEY)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupType")
                .name("type")
                .operators("blcOperators_Enumeration")
                .options("blcOptions_FulfillmentType")
                .type(SupportedFieldType.BROADLEAF_ENUMERATION)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupMerchandiseTotal")
                .name("merchandiseTotal")
                .operators("blcOperators_Numeric")
                .options("[]")
                .type(SupportedFieldType.MONEY)
                .build());
        fields.add(new FieldData.Builder()
                .label("rule_fulfillmentGroupFulfillmentOption")
                .name("fulfillmentOption.name")
                .operators("blcOperators_Text")
                .options("[]")
                .type(SupportedFieldType.STRING)
                .build());
    }

    @Override
    public String getName() {
        return RuleIdentifier.FULFILLMENTGROUP;
    }

    @Override
    public String getDtoClassName() {
        return "org.broadleafcommerce.core.order.domain.FulfillmentGroupImpl";
    }
}