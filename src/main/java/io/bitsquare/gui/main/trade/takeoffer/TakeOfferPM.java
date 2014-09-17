/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.gui.main.trade.takeoffer;

import io.bitsquare.btc.WalletFacade;
import io.bitsquare.gui.PresentationModel;
import io.bitsquare.gui.main.trade.OrderBookInfo;
import io.bitsquare.gui.util.BSFormatter;
import io.bitsquare.gui.util.validation.BtcValidator;
import io.bitsquare.gui.util.validation.InputValidator;
import io.bitsquare.locale.BSResources;
import io.bitsquare.trade.Direction;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.Coin;

import javax.inject.Inject;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.bitsquare.gui.util.BSFormatter.*;
import static javafx.beans.binding.Bindings.createStringBinding;

class TakeOfferPM extends PresentationModel<TakeOfferModel> {
    private static final Logger log = LoggerFactory.getLogger(TakeOfferPM.class);

    private String offerFee;
    private String networkFee;
    private String fiatCode;
    private String minAmount;
    private String price;
    private String directionLabel;
    private String collateralLabel;
    private String bankAccountType;
    private String bankAccountCurrency;
    private String bankAccountCounty;
    private String acceptedCountries;
    private String acceptedLanguages;
    private String acceptedArbitrators;
    private String addressAsString;
    private String paymentLabel;

    // Needed for the addressTextField
    final ObjectProperty<Address> address = new SimpleObjectProperty<>();

    private final BtcValidator btcValidator;

    final StringProperty amount = new SimpleStringProperty();
    final StringProperty volume = new SimpleStringProperty();
    final StringProperty collateral = new SimpleStringProperty();
    final StringProperty totalToPay = new SimpleStringProperty();
    final StringProperty transactionId = new SimpleStringProperty();
    final StringProperty requestTakeOfferErrorMessage = new SimpleStringProperty();
    final StringProperty btcCode = new SimpleStringProperty();


    final BooleanProperty isTakeOfferButtonVisible = new SimpleBooleanProperty(false);
    final BooleanProperty isTakeOfferButtonDisabled = new SimpleBooleanProperty(true);
    final BooleanProperty showWarningInvalidBtcDecimalPlaces = new SimpleBooleanProperty();
    final BooleanProperty showTransactionPublishedScreen = new SimpleBooleanProperty();

    final ObjectProperty<InputValidator.ValidationResult> amountValidationResult = new SimpleObjectProperty<>();

    // Needed for the addressTextField
    final ObjectProperty<Coin> totalToPayAsCoin = new SimpleObjectProperty<>();


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Constructor
    ///////////////////////////////////////////////////////////////////////////////////////////

    // non private for testing
    @Inject
    TakeOfferPM(TakeOfferModel model, BtcValidator btcValidator) {
        super(model);

        this.btcValidator = btcValidator;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Lifecycle
    ///////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void initialize() {
        super.initialize();

        // static
        offerFee = formatCoinWithCode(model.offerFeeAsCoin.get());
        networkFee = formatCoinWithCode(model.networkFeeAsCoin.get());

        setupBindings();
        setupListeners();
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void activate() {
        super.activate();
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void deactivate() {
        super.deactivate();
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    public void terminate() {
        super.terminate();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Setter
    ///////////////////////////////////////////////////////////////////////////////////////////

    // setOrderBookFilter is a one time call
    void setOrderBookInfo(@NotNull OrderBookInfo orderBookInfo) {
        model.setOrderBookInfo(orderBookInfo);

        directionLabel = orderBookInfo.getDirection() == Direction.BUY ?
                BSResources.get("shared.buy") : BSResources.get("shared.sell");

        fiatCode = orderBookInfo.getOffer().getCurrency().getCurrencyCode();
        model.priceAsFiat.set(orderBookInfo.getOffer().getPrice());
        model.minAmountAsCoin.set(orderBookInfo.getOffer().getMinAmount());
        if (orderBookInfo.getAmount() != null &&
                isBtcInputValid(orderBookInfo.getAmount().toPlainString()).isValid &&
                !orderBookInfo.getAmount().isGreaterThan(orderBookInfo.getOffer().getAmount())) {
            model.amountAsCoin.set(orderBookInfo.getAmount());
        }
        else {
            model.amountAsCoin.set(orderBookInfo.getOffer().getAmount());
        }
        model.volumeAsFiat.set(orderBookInfo.getOffer().getVolumeByAmount(model.amountAsCoin.get()));

        minAmount = BSFormatter.formatCoinWithCode(orderBookInfo.getOffer().getMinAmount());
        price = BSFormatter.formatFiatWithCode(orderBookInfo.getOffer().getPrice());

        paymentLabel = BSResources.get("takeOffer.fundsBox.paymentLabel", orderBookInfo.getOffer().getId());
        if (model.getAddressEntry() != null) {
            addressAsString = model.getAddressEntry().getAddress().toString();
            address.set(model.getAddressEntry().getAddress());
        }
        collateralLabel = BSResources.get("takeOffer.fundsBox.collateral",
                BSFormatter.formatCollateralPercent(orderBookInfo.getOffer().getCollateral()));

        acceptedCountries = BSFormatter.countryLocalesToString(orderBookInfo.getOffer().getAcceptedCountries());
        acceptedLanguages = BSFormatter.languageLocalesToString(orderBookInfo.getOffer().getAcceptedLanguageLocales());
        acceptedArbitrators = BSFormatter.arbitratorsToString(orderBookInfo.getOffer().getArbitrators());
        bankAccountType = BSResources.get(orderBookInfo.getOffer().getBankAccountType().toString());
        bankAccountCurrency = BSResources.get(orderBookInfo.getOffer().getCurrency().getDisplayName());
        bankAccountCounty = BSResources.get(orderBookInfo.getOffer().getBankAccountCountry().getName());
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI actions
    ///////////////////////////////////////////////////////////////////////////////////////////

    void takeOffer() {
        model.requestTakeOfferErrorMessage.set(null);
        model.requestTakeOfferSuccess.set(false);

        isTakeOfferButtonDisabled.set(true);

        model.takeOffer();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI events
    ///////////////////////////////////////////////////////////////////////////////////////////

    void onShowPayFundsScreen() {
        isTakeOfferButtonVisible.set(true);
    }

    // On focus out we do validation and apply the data to the model 
    void onFocusOutAmountTextField(Boolean oldValue, Boolean newValue, String userInput) {
        if (oldValue && !newValue) {
            InputValidator.ValidationResult result = isBtcInputValid(amount.get());
            amountValidationResult.set(result);
            if (result.isValid) {
                showWarningInvalidBtcDecimalPlaces.set(!hasBtcValidDecimals(userInput));
                // only allow max 4 decimal places for btc values
                setAmountToModel();
                // reformat input
                amount.set(formatCoin(model.amountAsCoin.get()));

                calculateVolume();
            }
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Getters
    ///////////////////////////////////////////////////////////////////////////////////////////

    WalletFacade getWalletFacade() {
        return model.getWalletFacade();
    }

    String getOfferFee() {
        return offerFee;
    }

    String getNetworkFee() {
        return networkFee;
    }

    String getFiatCode() {
        return fiatCode;
    }

    String getMinAmount() {
        return minAmount;
    }

    String getPrice() {
        return price;
    }

    String getDirectionLabel() {
        return directionLabel;
    }

    String getCollateralLabel() {
        return collateralLabel;
    }

    String getBankAccountType() {
        return bankAccountType;
    }

    String getBankAccountCurrency() {
        return bankAccountCurrency;
    }

    String getBankAccountCounty() {
        return bankAccountCounty;
    }

    String getAcceptedCountries() {
        return acceptedCountries;
    }

    String getAcceptedLanguages() {
        return acceptedLanguages;
    }

    String getAcceptedArbitrators() {
        return acceptedArbitrators;
    }

    String getAddressAsString() {
        return addressAsString;
    }

    String getPaymentLabel() {
        return paymentLabel;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Private
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void setupListeners() {
        // Bidirectional bindings are used for all input fields: amount, price, volume and minAmount
        // We do volume/amount calculation during input, so user has immediate feedback
        amount.addListener((ov, oldValue, newValue) -> {
            if (isBtcInputValid(newValue).isValid) {
                setAmountToModel();
                calculateVolume();
                model.calculateTotalToPay();
                model.calculateCollateral();
            }
            validateInput();
        });

        model.isWalletFunded.addListener((ov, oldValue, newValue) -> {
            if (newValue)
                validateInput();
        });

        // Binding with Bindings.createObjectBinding does not work because of bi-directional binding
        model.amountAsCoin.addListener((ov, oldValue, newValue) -> amount.set(formatCoin(newValue)));

        model.requestTakeOfferErrorMessage.addListener((ov, oldValue, newValue) -> {
            if (newValue != null)
                isTakeOfferButtonDisabled.set(false);
        });
        model.requestTakeOfferSuccess.addListener((ov, oldValue, newValue) -> isTakeOfferButtonVisible.set
                (!newValue));
    }

    private void setupBindings() {
        volume.bind(createStringBinding(() -> formatFiatWithCode(model.volumeAsFiat.get()), model.volumeAsFiat));
        totalToPay.bind(createStringBinding(() -> formatCoinWithCode(model.totalToPayAsCoin.get()),
                model.totalToPayAsCoin));
        collateral.bind(createStringBinding(() -> formatCoinWithCode(model.collateralAsCoin.get()),
                model.collateralAsCoin));

        totalToPayAsCoin.bind(model.totalToPayAsCoin);

        requestTakeOfferErrorMessage.bind(model.requestTakeOfferErrorMessage);
        showTransactionPublishedScreen.bind(model.requestTakeOfferSuccess);
        transactionId.bind(model.transactionId);

        btcCode.bind(model.btcCode);
    }

    private void calculateVolume() {
        setAmountToModel();
        model.calculateVolume();
    }

    private void setAmountToModel() {
        model.amountAsCoin.set(parseToCoinWith4Decimals(amount.get()));
    }

    private void validateInput() {
        isTakeOfferButtonDisabled.set(!(isBtcInputValid(amount.get()).isValid &&
                        model.isMinAmountLessOrEqualAmount() &&
                        model.isWalletFunded.get())
        );
    }

    private InputValidator.ValidationResult isBtcInputValid(String input) {
        return btcValidator.validate(input);
    }
}