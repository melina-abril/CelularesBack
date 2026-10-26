package com.ventacelulares.modules.checkout.application;

import com.ventacelulares.modules.checkout.application.dto.CartResponse;
import com.ventacelulares.modules.checkout.application.dto.DeliveryOptionsResponse;
import com.ventacelulares.modules.checkout.application.dto.OnlineOrderResponse;
import com.ventacelulares.modules.checkout.application.dto.PlaceOrderRequest;
import com.ventacelulares.modules.checkout.application.dto.ReservationResponse;
import com.ventacelulares.modules.checkout.application.dto.ReserveUnitRequest;
import com.ventacelulares.modules.checkout.application.dto.UpdateCartRequest;
import com.ventacelulares.modules.checkout.domain.CheckoutRepository;
import com.ventacelulares.modules.checkout.domain.CheckoutRepository.Accessory;
import com.ventacelulares.modules.checkout.domain.CheckoutRepository.Cart;
import com.ventacelulares.modules.checkout.domain.CheckoutRepository.Product;
import com.ventacelulares.modules.checkout.domain.CheckoutRepository.Reservation;
import com.ventacelulares.shared.exception.BusinessRuleException;
import com.ventacelulares.shared.exception.ConflictException;
import com.ventacelulares.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {
    private static final BigDecimal SHIPPING_LOCAL = new BigDecimal("9500.00");
    private static final BigDecimal SHIPPING_OTHER = new BigDecimal("19000.00");
    private static final BigDecimal VOUCHER_VALUE = new BigDecimal("100000.00");
    private final CheckoutRepository repository;
    private final CurrentUser currentUser;
    private final Clock clock;

    public CheckoutService(CheckoutRepository repository, CurrentUser currentUser, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    private ZonedDateTime now() {
        return ZonedDateTime.now(clock);
    }

    private String customer() {
        return currentUser.name();
    }

    private static CartResponse.AccessoryResponse accessoryResponse(Accessory accessory) {
        return new CartResponse.AccessoryResponse(accessory.id(), accessory.name(), accessory.price());
    }

    private BigDecimal accessoryTotal(Cart cart) {
        return cart.accessoryIds().stream().map(repository::accessory).map(Accessory::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    private CartResponse mapCart(Cart cart) {
        List<CartResponse.AccessoryResponse> suggested = repository.accessories().stream()
                .filter(item -> !cart.accessoryIds().contains(item.id()))
                .map(CheckoutService::accessoryResponse).toList();
        List<CartResponse.AccessoryResponse> selected = cart.accessoryIds().stream()
                .map(repository::accessory).map(CheckoutService::accessoryResponse).toList();
        if (cart.variantId() == null) {
            return new CartResponse(null, null, null, null, null, selected, suggested, null, BigDecimal.ZERO.setScale(2));
        }
        Product product = repository.product(cart.variantId());
        return new CartResponse(product.variantId(), product.model(), product.color(), product.memory(),
                product.cashPrice(), selected, suggested, cart.voucherCode(),
                product.cashPrice().add(accessoryTotal(cart)));
    }

    public CartResponse getCart() {
        // Respuesta dummy del adaptador en memoria; reemplazar al conectar persistencia.
        return mapCart(repository.cart(customer()));
    }

    public CartResponse updateCart(UpdateCartRequest request) {
        repository.product(request.variantId());
        if (request.accessoryIds().size() != request.accessoryIds().stream().distinct().count()) {
            throw new BusinessRuleException("BR-04", "No se puede agregar dos veces el mismo accesorio");
        }
        request.accessoryIds().forEach(repository::accessory);
        validateVoucher(request.voucherCode());
        return mapCart(repository.updateCart(customer(), new Cart(request.variantId(), request.accessoryIds(), request.voucherCode())));
    }

    private void validateVoucher(String code) {
        // Vale dummy vigente para demostración; reemplazar por validación del módulo de canje mediante un puerto.
        if (code != null && !code.isBlank() && !"CANJE-DEMO-001".equals(code)) {
            throw new BusinessRuleException("BR-04", "El vale no existe o venció");
        }
    }

    private BigDecimal shippingCost(String postalCode) {
        if (postalCode == null || !postalCode.matches("[0-9]{4}")) {
            throw new BusinessRuleException("BR-04", "El código postal debe tener cuatro dígitos");
        }
        int code = Integer.parseInt(postalCode);
        if (code >= 1000 && code <= 1999) return SHIPPING_LOCAL;
        if (code >= 2000 && code <= 8999) return SHIPPING_OTHER;
        throw new BusinessRuleException("BR-04", "El envío no está disponible para ese código postal");
    }

    public DeliveryOptionsResponse getDeliveryOptions(String postalCode) {
        Cart cart = repository.cart(customer());
        List<DeliveryOptionsResponse.PickupOption> branches = cart.variantId() == null ? List.of()
                : repository.availability(cart.variantId(), now()).stream()
                        .map(item -> new DeliveryOptionsResponse.PickupOption(item.branch(), item.branch().displayName()))
                        .toList();
        DeliveryOptionsResponse.ShippingOption shipping = null;
        if (postalCode != null && !postalCode.isBlank()) {
            try {
                shipping = new DeliveryOptionsResponse.ShippingOption(postalCode, true, shippingCost(postalCode));
            } catch (BusinessRuleException invalidPostalCode) {
                shipping = new DeliveryOptionsResponse.ShippingOption(postalCode, false, null);
            }
        }
        return new DeliveryOptionsResponse(branches, shipping);
    }

    public ReservationResponse reserveUnit(ReserveUnitRequest request) {
        Reservation reservation = repository.reserve(customer(), request.variantId(), request.branch(), now());
        return new ReservationResponse(reservation.id(), reservation.variantId(), reservation.branch(), reservation.expiresAt());
    }

    public OnlineOrderResponse placeOnlineOrder(PlaceOrderRequest request) {
        ZonedDateTime timestamp = now();
        Reservation reservation = repository.reservation(request.reservationId(), customer(), timestamp);
        Cart cart = repository.cart(customer());
        if (!reservation.variantId().equals(cart.variantId())) throw new ConflictException("La reserva no coincide con el carrito");
        if (!"A".equals(request.invoiceType()) && !"B".equals(request.invoiceType())) {
            throw new BusinessRuleException("BR-08", "El tipo de factura debe ser A o B");
        }
        if ("A".equals(request.invoiceType()) && (request.cuit() == null || !request.cuit().matches("[0-9]{11}"))) {
            throw new BusinessRuleException("BR-08", "La factura A requiere CUIT de once dígitos");
        }
        BigDecimal shipping = BigDecimal.ZERO.setScale(2);
        if ("ENVIO".equals(request.deliveryMethod())) {
            if (request.address() == null || request.address().isBlank()) {
                throw new BusinessRuleException("BR-04", "El envío requiere domicilio");
            }
            shipping = shippingCost(request.postalCode());
        } else if (!"RETIRO".equals(request.deliveryMethod())) {
            throw new BusinessRuleException("BR-04", "El método de entrega no es válido");
        }
        if (!"TARJETA".equals(request.paymentMethod()) && !"TRANSFERENCIA".equals(request.paymentMethod())) {
            throw new BusinessRuleException("BR-03", "El medio de pago no es válido");
        }
        if ("TARJETA".equals(request.paymentMethod()) && (request.paymentToken() == null || request.paymentToken().isBlank())) {
            throw new BusinessRuleException("BR-03", "La tarjeta requiere un token de la pasarela");
        }
        if ("TRANSFERENCIA".equals(request.paymentMethod()) && request.paymentToken() != null && !request.paymentToken().isBlank()) {
            throw new BusinessRuleException("BR-03", "Una transferencia no usa token de tarjeta");
        }
        if ("TRANSFERENCIA".equals(request.paymentMethod()) && request.installments() != 1) {
            throw new BusinessRuleException("BR-03", "La transferencia se paga en una cuota");
        }
        if ("TARJETA".equals(request.paymentMethod()) && !List.of(1, 3, 6, 9, 12, 18).contains(request.installments())) {
            throw new BusinessRuleException("BR-03", "Cantidad de cuotas no disponible");
        }
        Product product = repository.product(cart.variantId());
        validateVoucher(cart.voucherCode());
        BigDecimal discount = cart.voucherCode() == null || cart.voucherCode().isBlank()
                ? BigDecimal.ZERO.setScale(2) : VOUCHER_VALUE;
        if (discount.compareTo(product.cashPrice().multiply(new BigDecimal("0.40"))) > 0) {
            throw new BusinessRuleException("BR-04", "El canje no puede superar el 40% del precio contado del equipo");
        }
        BigDecimal accessories = accessoryTotal(cart);
        BigDecimal cashTotal = product.cashPrice().add(accessories).add(shipping).subtract(discount);
        // Simulación dummy: sin promociones bancarias, las cuotas llevan 10% de recargo.
        BigDecimal total = request.installments() > 1
                ? cashTotal.multiply(new BigDecimal("1.10")).setScale(2, java.math.RoundingMode.HALF_EVEN)
                : cashTotal;
        if (total.compareTo(cashTotal) < 0) throw new BusinessRuleException("BR-03", "El total financiado no puede ser menor al contado");
        CheckoutRepository.Order order = repository.placeOrder(customer(), reservation.id(), request.dni(),
                request.invoiceType(), request.deliveryMethod(), request.postalCode(), request.paymentMethod(), total, timestamp);
        return new OnlineOrderResponse(order.id(), order.variantId(), order.status(), order.invoiceType(),
                order.deliveryMethod(), order.branch(), order.paymentMethod(), product.cashPrice(),
                accessories, shipping, discount, order.total(), order.createdAt(), order.transferDeadline());
    }
}