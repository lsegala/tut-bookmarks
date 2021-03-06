/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contaazul.bankslips;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * @author Josh Long
 */
// tag::code[]
@RestController
@Api(value="bankslips", description="Operations pertaining to bankslips")
@RequestMapping("/rest/bankslips")
class BankSlipRestController {

	private final BankslipService bankslipService;

	@Autowired
	BankSlipRestController(BankslipService bankslipService) {
		this.bankslipService = bankslipService;
	}

	@ApiOperation(value = "Read a BankSlip and calculate tax if is late", response = BankSlip.class)
	@ApiResponses({
			@ApiResponse(code = 200, message = "Ok"),
			@ApiResponse(code = 404, message = "Bankslip not found with the specified id")
	})
	@GetMapping("/{id}")
	Resource<BankSlip> readBankSlip(@PathVariable String id) {
		return this.bankslipService.findById(id)
				.map(b -> toResource(b))
				.orElseThrow(BankSlipNotFoundException::new);
	}

	private Resource<BankSlip> toResource(BankSlip b) {
		return new Resource<>(b,
				new Link(ServletUriComponentsBuilder
						.fromCurrentRequest()
						.path("/{id}")
						.buildAndExpand(b.getId())
						.toUriString(), "self"),
				linkTo(methodOn(BankSlipRestController.class).readBankSlips()).withRel("bankslips-uri"));
	}

	@ApiOperation(value = "Receave a Bankslip and inserts in a database", response = BankSlip.class)
	@ApiResponses({
			@ApiResponse(code = 201, message = "Bankslip created"),
			@ApiResponse(code = 400, message = "Bankslip not provided in the request body"),
			@ApiResponse(code = 422, message = "Invalid bankslip provided")
	})
	@PostMapping
	ResponseEntity<Resource<BankSlip>> add(@RequestBody BankSlip bankSlip) {
		return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResource(this.bankslipService.save(bankSlip)));
	}

	@ApiOperation(value = "List all bankslips", response = BankSlip.class)
	@GetMapping
	List<Resource<BankSlip>> readBankSlips() {
		return this.bankslipService
			.findAll()
			.stream()
			.map(b -> toResource(b))
			.collect(Collectors.toList());
	}

	@ApiOperation(value = "Pay an bankslip", response = BankSlip.class)
	@ApiResponses({
			@ApiResponse(code = 204, message = "No content"),
			@ApiResponse(code = 404, message = "Bankslip not found with the specified id")
	})
	@PostMapping("/{id}/payments")
	ResponseEntity doPayment(@PathVariable String id, @RequestBody BankSlip body){
		this.bankslipService.doPayment(id, body.getPaymentDate());
		return ResponseEntity.noContent().build();
	}

	@ApiOperation(value = "Cancel an bankslip", response = BankSlip.class)
	@ApiResponses({
			@ApiResponse(code = 204, message = "Bankslip canceled"),
			@ApiResponse(code = 404, message = "Bankslip not found with the specified id")
	})
	@DeleteMapping("/{id}")
	ResponseEntity cancelPayment(@PathVariable String id){
		this.bankslipService.cancelPayment(id);
		return ResponseEntity.noContent().build();
	}
}
// end::code[]
