package com.example.demo.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.example.demo.exception.PaymentNotFoundException;
import com.example.demo.model.Email;
import com.example.demo.model.Payment;
import com.example.demo.model.SMS;
import com.example.demo.repository.PaymentRepository;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/")
public class PaymentController {
	@Autowired
	private PaymentRepository paymentRepository;

	@Autowired
	private JavaMailSender mailsender;
	//get all payment
	@GetMapping("/payment")
	public List<Payment> getAllEmployees() {
		return paymentRepository.findAll();
	}

	public static final String ACCOUNT_SID = "AC6e6be5415871a0e7344e4144d8149d9e";
    public static final String AUTH_TOKEN = "869daa1a0911bcb2ecbc8d3a5baf89eb";
    public static final String TWILIO_NUMBER = "+17207307365";
	
    //add new payment
	@PostMapping("/payment")
	public Payment creatPayment(@RequestBody Payment payment) {
		Payment res = paymentRepository.save(payment);
		String to = payment.getEmail();
		String ReceiversMobile = payment.getMobile();
		String subject = "Payment Confirmation";
		String body = "Thank you ,Your payment Rs."+payment.getAmount() + " has been sussessfully updated!";
		SMS sms = new SMS(ReceiversMobile,body);
		Email email = new Email(to,subject,body);
		sendVerificationEmail(email);
		sendSMS(sms);
		
		return res;
	}
	//send verificateion email
	private void sendVerificationEmail(Email email) {
		SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
		simpleMailMessage.setFrom("applicationframeworkproject@gmail.com");
		simpleMailMessage.setTo(email.getTo());
		simpleMailMessage.setSubject(email.getSubject());
		simpleMailMessage.setText(email.getBody());
		mailsender.send(simpleMailMessage);
		
	}
	//send sms
    public void sendSMS(SMS sms) {
        try {
            TwilioRestClient client = new TwilioRestClient(ACCOUNT_SID, AUTH_TOKEN);
            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new BasicNameValuePair("Body", sms.getSMSBody()));
            params.add(new BasicNameValuePair("To", sms.getReceivedNumber()));
            params.add(new BasicNameValuePair("From", TWILIO_NUMBER));

            MessageFactory messageFactory = client.getAccount().getMessageFactory();
            Message message = messageFactory.create(params);
            System.out.println(message.getSid());
        } 
        catch (TwilioRestException e) {
            System.out.println(e.getErrorMessage());
        }
    }

	//get payment using id
	@GetMapping("/payment/{id}")
	public ResponseEntity<Payment> getEmployeeById(@PathVariable Long id) {
		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new PaymentNotFoundException("Employee not exist with id :" + id));
		return ResponseEntity.ok(payment);
	}

	//update payment using id
	@PutMapping("/payment/{id}")
	public ResponseEntity<Payment> updateEmployee(@PathVariable Long id, @RequestBody Payment paymentDetails) {
		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new PaymentNotFoundException("Employee not exist with id :" + id));

		payment.setCardNumber(paymentDetails.getCardNumber());
		payment.setCardHolderName(paymentDetails.getCardHolderName());
		payment.setCvc(paymentDetails.getCvc());
		payment.setExpireDate(paymentDetails.getExpireDate());

		Payment updatePayment = paymentRepository.save(payment);
		return ResponseEntity.ok(updatePayment);
	}
	//delete payment using id
	@DeleteMapping("/payment/{id}")
	public ResponseEntity<Map<String, Boolean>> deleteEmployee(@PathVariable Long id) {
		Payment payment = paymentRepository.findById(id)
				.orElseThrow(() -> new PaymentNotFoundException("Employee not exist with id :" + id));

		paymentRepository.delete(payment);
		Map<String, Boolean> response = new HashMap<>();
		response.put("deleted", Boolean.TRUE);
		return ResponseEntity.ok(response);
	}
}