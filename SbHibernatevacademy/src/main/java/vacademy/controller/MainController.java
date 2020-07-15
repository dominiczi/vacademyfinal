package vacademy.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
import vacademy.dao.OrderDAO;
import vacademy.dao.ProductDAO;
import vacademy.entity.Product;
import vacademy.form.CustomerForm;
import vacademy.model.CartInfo;
import vacademy.model.CustomerInfo;
import vacademy.model.ProductInfo;
import vacademy.pagination.PaginationResult;
import vacademy.utils.Utils;
import vacademy.validator.CustomerFormValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
@Controller
@Transactional
public class MainController {
 
   @Autowired
   private OrderDAO orderDAO;
 
   @Autowired
   private ProductDAO productDAO;
 
   @Autowired
   private CustomerFormValidator customerFormValidator;
 
   @InitBinder
   public void myInitBinder(WebDataBinder dataBinder) {
      Object target = dataBinder.getTarget();
      if (target == null) {
         return;
      }
      System.out.println("Target=" + target);
 
      // Case update quantity in cart
      // (@ModelAttribute("cartForm") @Validated CartInfo cartForm)
      if (target.getClass() == CartInfo.class) {
 
      }
 
      // Case save customer information.
      // (@ModelAttribute @Validated CustomerInfo customerForm)
      else if (target.getClass() == CustomerForm.class) {
         dataBinder.setValidator(customerFormValidator);
      }
 
   }
 
   @RequestMapping("/403")
   public String accessDenied() {
      return "/403";
   }
 
   @RequestMapping("/")
   public String home() {
      return "index";
   }
 
   // Product List
   @RequestMapping({ "/courseList" })
   public String listProductHandler(Model model, //
         @RequestParam(value = "name", defaultValue = "") String likeName,
         @RequestParam(value = "page", defaultValue = "1") int page) {
      final int maxResult = 5;
      final int maxNavigationPage = 10;
 
      PaginationResult<ProductInfo> result = productDAO.queryProducts(page, //
            maxResult, maxNavigationPage, likeName);
 
      model.addAttribute("paginationProducts", result);
      return "courseList";
   }
 
   @RequestMapping({ "/buyProduct" })
   public String listProductHandler(HttpServletRequest request, Model model, //
         @RequestParam(value = "code", defaultValue = "") String code) {
 
      Product product = null;
      if (code != null && code.length() > 0) {
         product = productDAO.findProduct(code);
      }
      if (product != null) {
 
         //
         CartInfo cartInfo = Utils.getCartInSession(request);
 
         ProductInfo productInfo = new ProductInfo(product);
 
         cartInfo.addProduct(productInfo, 1);
      }
 
      return "redirect:/vacademyCart";
   }
 
   @RequestMapping({ "/vacademyCartRemoveProduct" })
   public String removeProductHandler(HttpServletRequest request, Model model, //
         @RequestParam(value = "code", defaultValue = "") String code) {
      Product product = null;
      if (code != null && code.length() > 0) {
         product = productDAO.findProduct(code);
      }
      if (product != null) {
 
         CartInfo cartInfo = Utils.getCartInSession(request);
 
         ProductInfo productInfo = new ProductInfo(product);
 
         cartInfo.removeProduct(productInfo);
 
      }
 
      return "redirect:/vacademyCart";
   }
 
   // POST: Update quantity for product in cart
   @RequestMapping(value = { "/vacademyCart" }, method = RequestMethod.POST)
   public String vacademyCartUpdateQty(HttpServletRequest request, //
         Model model, //
         @ModelAttribute("cartForm") CartInfo cartForm) {
 
      CartInfo cartInfo = Utils.getCartInSession(request);
      cartInfo.updateQuantity(cartForm);
 
      return "redirect:/vacademyCart";
   }
 
   // GET: Show cart.
   @RequestMapping(value = { "/vacademyCart" }, method = RequestMethod.GET)
   public String vacademyCartHandler(HttpServletRequest request, Model model) {
      CartInfo myCart = Utils.getCartInSession(request);
 
      model.addAttribute("cartForm", myCart);
      return "vacademyCart";
   }
 
   // GET: Enter customer information.
   @RequestMapping(value = { "/vacademyCartCustomer" }, method = RequestMethod.GET)
   public String vacademyCartCustomerForm(HttpServletRequest request, Model model) {
 
      CartInfo cartInfo = Utils.getCartInSession(request);
 
      if (cartInfo.isEmpty()) {
 
         return "redirect:/vacademyCart";
      }
      CustomerInfo customerInfo = cartInfo.getCustomerInfo();
 
      CustomerForm customerForm = new CustomerForm(customerInfo);
 
      model.addAttribute("customerForm", customerForm);
 
      return "vacademyCartCustomer";
   }
 
   // POST: Save customer information.
   @RequestMapping(value = { "/vacademyCartCustomer" }, method = RequestMethod.POST)
   public String vacademyCartCustomerSave(HttpServletRequest request, //
         Model model, //
         @ModelAttribute("customerForm") @Validated CustomerForm customerForm, //
         BindingResult result, //
         final RedirectAttributes redirectAttributes) {
 
      if (result.hasErrors()) {
         customerForm.setValid(false);
         // Forward to reenter customer info.
         return "vacademyCartCustomer";
      }
 
      customerForm.setValid(true);
      CartInfo cartInfo = Utils.getCartInSession(request);
      CustomerInfo customerInfo = new CustomerInfo(customerForm);
      cartInfo.setCustomerInfo(customerInfo);
 
      return "redirect:/vacademyCartConfirmation";
   }
 
   // GET: Show information to confirm.
   @RequestMapping(value = { "/vacademyCartConfirmation" }, method = RequestMethod.GET)
   public String vacademyCartConfirmationReview(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);
 
      if (cartInfo == null || cartInfo.isEmpty()) {
 
         return "redirect:/vacademyCart";
      } else if (!cartInfo.isValidCustomer()) {
 
         return "redirect:/vacademyCartCustomer";
      }
      model.addAttribute("myCart", cartInfo);
 
      return "vacademyCartConfirmation";
   }
 
   // POST: Submit Cart (Save)
   @RequestMapping(value = { "/vacademyCartConfirmation" }, method = RequestMethod.POST)
 
   public String vacademyCartConfirmationSave(HttpServletRequest request, Model model) {
      CartInfo cartInfo = Utils.getCartInSession(request);
 
      if (cartInfo.isEmpty()) {
 
         return "redirect:/vacademyCart";
      } else if (!cartInfo.isValidCustomer()) {
 
         return "redirect:/vacademyCartCustomer";
      }
      try {
         orderDAO.saveOrder(cartInfo);
      } catch (Exception e) {
 
         return "vacademyCartConfirmation";
      }
 
      // Remove Cart from Session.
      Utils.removeCartInSession(request);
 
      // Store last cart.
      Utils.storeLastOrderedCartInSession(request, cartInfo);
 
      return "redirect:/vacademyCartFinalize";
   }
 
   @RequestMapping(value = { "/vacademyCartFinalize" }, method = RequestMethod.GET)
   public String vacademyCartFinalize(HttpServletRequest request, Model model) {
 
      CartInfo lastOrderedCart = Utils.getLastOrderedCartInSession(request);
 
      if (lastOrderedCart == null) {
         return "redirect:/vacademyCart";
      }
      model.addAttribute("lastOrderedCart", lastOrderedCart);
      return "vacademyCartFinalize";
   }
 
   @RequestMapping(value = { "/productImage" }, method = RequestMethod.GET)
   public void productImage(HttpServletRequest request, HttpServletResponse response, Model model,
         @RequestParam("code") String code) throws IOException {
      Product product = null;
      if (code != null) {
         product = this.productDAO.findProduct(code);
      }
      if (product != null && product.getImage() != null) {
         response.setContentType("image/jpeg, image/jpg, image/png, image/gif");
         response.getOutputStream().write(product.getImage());
      }
      response.getOutputStream().close();
   }
 
}