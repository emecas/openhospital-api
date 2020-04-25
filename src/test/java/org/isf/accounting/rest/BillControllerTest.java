package org.isf.accounting.rest;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.isf.shared.mapper.OHModelMapper.getObjectMapper;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.isf.accounting.dto.BillDTO;
import org.isf.accounting.dto.BillItemsDTO;
import org.isf.accounting.dto.BillPaymentsDTO;
import org.isf.accounting.dto.FullBillDTO;
import org.isf.accounting.manager.BillBrowserManager;
import org.isf.accounting.model.Bill;
import org.isf.accounting.model.BillItems;
import org.isf.accounting.model.BillPayments;
import org.isf.accounting.test.TestBill;
import org.isf.accounting.test.TestBillItems;
import org.isf.accounting.test.TestBillPayments;
import org.isf.patient.dto.PatientDTO;
import org.isf.patient.manager.PatientBrowserManager;
import org.isf.patient.model.Patient;
import org.isf.patient.test.TestPatient;
import org.isf.priceslist.manager.PriceListManager;
import org.isf.priceslist.model.PriceList;
import org.isf.priceslist.test.TestPriceList;
import org.isf.shared.exceptions.OHAPIException;
import org.isf.shared.exceptions.OHResponseEntityExceptionHandler;
import org.isf.shared.mapper.OHModelMapper;
import org.isf.utils.exception.OHException;
import org.isf.utils.exception.OHServiceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Emerson Castaneda
 *
 */

public class BillControllerTest {
	private final Logger logger = LoggerFactory.getLogger(BillControllerTest.class);
	
	@Mock
	protected BillBrowserManager billManagerMock;
	
	@Mock
	protected PriceListManager priceListManagerMock;
	
	@Mock
	protected PatientBrowserManager patientManagerMock;
	
    private MockMvc mockMvc;

    @Before
    public void setup() {
    	MockitoAnnotations.initMocks(this);
    	this.mockMvc = MockMvcBuilders
				.standaloneSetup(new BillController(billManagerMock, priceListManagerMock, patientManagerMock))
   				.setControllerAdvice(new OHResponseEntityExceptionHandler())
   				.build();
    }

	@Test
	public void when_post_bills_is_call_without_contentType_header_then_HttpMediaTypeNotSupportedException() throws Exception {
		String request = "/bills";
		
		MvcResult result = this.mockMvc
			.perform(post(request).content(new byte[]{'a', 'b', 'c'}))
			.andDo(log())
			.andExpect(status().is4xxClientError())
			.andExpect(status().isUnsupportedMediaType())
			.andExpect(content().string(anyOf(nullValue(), equalTo(""))))
			.andReturn();
		
		Optional<HttpMediaTypeNotSupportedException> exception = Optional.ofNullable((HttpMediaTypeNotSupportedException) result.getResolvedException());
		logger.debug("exception: {}", exception);
		exception.ifPresent( (se) -> assertThat(se, notNullValue()));
		exception.ifPresent( (se) -> assertThat(se, instanceOf(HttpMediaTypeNotSupportedException.class)));
	
	}
	
	@Test
	public void when_post_bills_is_call_with_empty_body_then_BadRequest_HttpMessageNotReadableException() throws Exception {
		String request = "/bills";
		String empty_body = "";
				
		MvcResult result = this.mockMvc
			.perform(
				post(request)
				.content(empty_body.getBytes())
				.contentType(MediaType.APPLICATION_JSON)
			)
			.andDo(log())
			.andExpect(status().is4xxClientError())
			.andExpect(status().isBadRequest())
			.andExpect(content().string(anyOf(nullValue(), equalTo(""))))
			.andReturn();
		
		Optional<HttpMessageNotReadableException> exception = Optional.ofNullable((HttpMessageNotReadableException) result.getResolvedException());
		logger.debug("exception: {}", exception);
		exception.ifPresent( (se) -> assertThat(se, notNullValue()));
		exception.ifPresent( (se) -> assertThat(se, instanceOf(HttpMessageNotReadableException.class)));
	}
	
	
	@Test
	public void when_post_patients_PatientBrowserManager_getPatient_returns_null_then_OHAPIException_BadRequest() throws Exception {
		String request = "/bills";
		FullBillDTO newFullBillDTO =  FullBillDTOHelper.setup();
		Integer id = 0;
		newFullBillDTO.getBill().setId(id);
		Integer code = 0;
		newFullBillDTO.getBill().getPatientDTO().setCode(code);
		
		newFullBillDTO.getBill().setPatient(true);
		
		String jsonNewFullBillDTO = FullBillDTOHelper.asJsonString(newFullBillDTO);
		System.out.println("JSON ---> " + jsonNewFullBillDTO);
		
		logger.info("JSON --> " +jsonNewFullBillDTO);
		
		when(patientManagerMock.getPatient(any(String.class))).thenReturn(null);
		
		MvcResult result = this.mockMvc
			.perform(
				post(request)
				.contentType(MediaType.APPLICATION_JSON)
				.content(FullBillDTOHelper.asJsonString(newFullBillDTO))
			)
			.andDo(log())
			.andDo(print())
			.andExpect(status().is4xxClientError())
			.andExpect(status().isBadRequest()) //TODO Create OHCreateAPIException
			.andExpect(content().string(containsString("Patient Not found!")))
			.andReturn();
		
		//TODO Create OHCreateAPIException
		Optional<OHAPIException> oHAPIException = Optional.ofNullable((OHAPIException) result.getResolvedException());
		logger.debug("oHAPIException: {}", oHAPIException);
		oHAPIException.ifPresent( (se) -> assertThat(se, notNullValue()));
		oHAPIException.ifPresent( (se) -> assertThat(se, instanceOf(OHAPIException.class)));
	}
		
	@Test
	public void when_put_bills_PatientBrowserManager_getPatient_returns_null_then_OHAPIException_BadRequest() throws Exception {
		Integer id = 123;
		String request = "/bills/{id}";
		
		FullBillDTO newFullBillDTO =  FullBillDTOHelper.setup();
		newFullBillDTO.getBill().setId(id);
		Integer code = 111;
		newFullBillDTO.getBill().getPatientDTO().setCode(code);
		newFullBillDTO.getBill().setPatient(true);
		String jsonNewFullBillDTO = FullBillDTOHelper.asJsonString(newFullBillDTO);
		System.out.println("JSON ---> " + jsonNewFullBillDTO);
		Bill bill = BillHelper.setup();
		when(patientManagerMock.getPatient(eq(bill.getPatName()))).thenReturn(null);
		when(billManagerMock.getBill(eq(id))).thenReturn(bill);
		when(billManagerMock.deleteBill(eq(bill))).thenReturn(true);
		
		MvcResult result = this.mockMvc
			.perform(
				put(request, id)
				.contentType(MediaType.APPLICATION_JSON)
				.content(FullBillDTOHelper.asJsonString(newFullBillDTO))
			)
			.andDo(log())
			.andDo(print())
			.andExpect(status().is4xxClientError())
			.andExpect(status().isBadRequest()) //TODO Create OHCreateAPIException
			.andExpect(content().string(containsString("Patient Not found!")))
			.andReturn();
		
		//TODO Create OHCreateAPIException
		Optional<OHAPIException> oHAPIException = Optional.ofNullable((OHAPIException) result.getResolvedException());
		logger.debug("oHAPIException: {}", oHAPIException);
		oHAPIException.ifPresent( (se) -> assertThat(se, notNullValue()));
		oHAPIException.ifPresent( (se) -> assertThat(se, instanceOf(OHAPIException.class)));
	}
	
	@Test
	public void when_put_bills_PatientBrowserManager_getPatient_returns_null_then_OK() throws Exception {
		Integer id = 123;
		String request = "/bills/{id}";
		FullBillDTO newFullBillDTO =  FullBillDTOHelper.setup();
		newFullBillDTO.getBill().setId(id);
		Integer code = 111;
		newFullBillDTO.getBill().getPatientDTO().setCode(code);
		newFullBillDTO.getBill().setPatient(true);
		String jsonNewFullBillDTO = FullBillDTOHelper.asJsonString(newFullBillDTO);
		Bill bill = BillHelper.setup();
		Patient patient = bill.getBillPatient();
		when(patientManagerMock.getPatient(any(String.class))).thenReturn(patient);
		when(billManagerMock.getBill(eq(id))).thenReturn(bill);
		ArrayList<PriceList> priceListList = new ArrayList<PriceList>();
		PriceList priceList = bill.getPriceList();
		priceList.setName("TestListNameToMatch");
		newFullBillDTO.getBill().setListName("TestListNameToMatch");
		priceListList.add(priceList);
		when(priceListManagerMock.getLists()).thenReturn(priceListList);
		
		//TODO open a ticket for suggesting refactoring for updateBill() method in order to accept generic List instead of ArrayList  like:
		// public boolean updateBill(Bill updateBill,
		//		List<BillItems> billItems, 
		//		List<BillPayments> billPayments) throws OHServiceException 
		//ArrayList<BillItems> billItemsArrayList = new ArrayList(newFullBillDTO.getBillItems());
		//billItemsArrayList.addAll(newFullBillDTO.getBillItems());
		
		ArrayList<BillItems> billItemsArrayList = BillItemsDTOHelper.toModelList(newFullBillDTO.getBillItems());
		
		ArrayList<BillPayments> billPaymentsArrayList = BillPaymentsDTOHelper.toModelList(newFullBillDTO.getBillPayments());
		
		//TODO  check eq(bill) case
		//when(billManagerMock.updateBill(eq(bill), eq(billItemsArrayList), eq(billPaymentsArrayList)))
		when(billManagerMock.updateBill(any(Bill.class), eq(billItemsArrayList), eq(billPaymentsArrayList)))
		.thenReturn(true);

		this.mockMvc
			.perform(
				put(request, id)
				.contentType(MediaType.APPLICATION_JSON)
				.content(FullBillDTOHelper.asJsonString(newFullBillDTO))
			)
			.andDo(log())
			.andDo(print())
			.andExpect(status().isCreated()) 
			.andExpect(content().string(containsString(FullBillDTOHelper.asJsonString(newFullBillDTO))))
			.andReturn();
	}

	
	
	
	public static <T> ArrayList<T> listToArrayList(List<T> myList) {
        ArrayList<T> arl = new ArrayList<>();
        for (Object object : myList) {
            arl.add((T) object);
        }
        return arl;

    }
	
	@Test
	public void when_get_items_with_existent_id_then_getItems_returns_items_and_OK() throws Exception {
		Integer id = 123;
		String request = "/bills/items/{bill_id}";
		
		FullBillDTO newFullBillDTO =  FullBillDTOHelper.setup();
		newFullBillDTO.getBill().setId(id);
		
		ArrayList<BillItems> itemsDTOSExpected = new ArrayList<BillItems>();
		itemsDTOSExpected.addAll(newFullBillDTO.getBillItems().stream().map(it-> getObjectMapper().map(it, BillItems.class)).collect(Collectors.toList()));
				
		when(billManagerMock.getItems(eq(id))).thenReturn(itemsDTOSExpected);
		
		this.mockMvc
			.perform(
					get(request, id)
					.contentType(MediaType.APPLICATION_JSON)
					)		
			.andDo(log())
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(new ObjectMapper().writeValueAsString(newFullBillDTO.getBillItems()))))
			.andReturn();
	}
	
	@Test
	public void when_get_items_with_existent_id_then_getItems_is_empty_and_isNoContent() throws Exception {
		Integer id = 123;
		String request = "/bills/items/{bill_id}";
		
		this.mockMvc
			.perform(
					get(request, id)
					.contentType(MediaType.APPLICATION_JSON)
					)		
			.andDo(log())
			.andExpect(status().isNoContent());
		
		
	}

	@Test
	public void when_get_bill_with_existent_id_then_response_BillDTO_and_OK() throws Exception {
		Integer id = 123;
		String request = "/bills/{id}";
		
		Bill bill = BillHelper.setup();
		bill.setId(id);
		
		//BillDTO billDTO = getObjectMapper().map(bill, BillDTO.class);
		
		BillDTO expectedBillDTO = BillDTOHelper.setup(id);
		
		//expectedBillDTO.setPatientDTO(null);
		
		//Integer code = 111;
		//expectedBillDTO.getBill().getPatientDTO().setCode(code);
		//expectedBillDTO.getBill().setPatient(true);		
				
		when(billManagerMock.getBill(eq(id))).thenReturn(bill);
		
		System.out.println(String.format("bill: %s", BillHelper.asJsonString(bill)));
		System.out.println(String.format("expectedBillDTO: %s", BillDTOHelper.asJsonString(expectedBillDTO)));
		
		MvcResult actualBillDTResponse  = this.mockMvc
			.perform(
					get(request, id)
					.contentType(MediaType.APPLICATION_JSON)
					)		
			.andDo(log())
			.andExpect(status().isOk())
			// TODO 1 .andExpect(content().string(containsString(BillDTOHelper.asJsonString(expectedBillDTO))))
			.andReturn();
		
		String actualStringResponseContent = actualBillDTResponse.getResponse().getContentAsString();
		
		System.out.println(String.format("actualStringResponseContent: %s", actualStringResponseContent));
	}

	@Test
	public void when_delete_bill_with_existent_id_then_response_true_and_OK() throws Exception {
		Integer id = 123;
		String request = "/bills/{id}";
		
		Bill bill = BillHelper.setup();
		
		when(billManagerMock.getBill(eq(id))).thenReturn(bill);
		
		when(billManagerMock.deleteBill(eq(bill))).thenReturn(true);
		
		this.mockMvc
			.perform(
					delete(request, id)
					.contentType(MediaType.APPLICATION_JSON)
					)		
			.andDo(log())
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("true")));
	}
	
	
	@Test
	public void when_get_bill_pending_affiliate_with_existent_patiend_code_then_response_List_of_BillDTO_and_OK() throws Exception {
		Integer code = 123;
		String request = "/bills/pending/affiliate?patient_code={code}";
		
		Bill bill1 = BillHelper.setup();
		int id1 = 1 ;
		bill1.setId(id1);
		Bill bill2 = BillHelper.setup();
		int id2 = 2 ;
		bill2.setId(id2);
		BillDTO expectedBillDTO1 = getObjectMapper().map(bill1, BillDTO.class);
		BillDTO expectedBillDTO2 = getObjectMapper().map(bill2, BillDTO.class);
		
		ArrayList<Bill> bills = new ArrayList<Bill>();
		bills.add(bill1);
		bills.add(bill2);

		when(billManagerMock.getPendingBillsAffiliate(eq(code))).thenReturn(bills);
		
		this.mockMvc
			.perform(
					get(request, code)
					.contentType(MediaType.APPLICATION_JSON)
					)		
			.andDo(log())
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(BillDTOHelper.asJsonString(expectedBillDTO1))))
			.andExpect(content().string(containsString(BillDTOHelper.asJsonString(expectedBillDTO2))))
			.andReturn();
	}

	
	@Test
	public void when_post_SearchBillsByPayments_with_a_list_of_existent_billsPaymentsDTO_then_response_List_of_BillDTO_and_OK() throws Exception {
		String request = "/bills/search/by/payments";
		
		Bill bill1 = BillHelper.setup();
		int id1 = 1 ;
		bill1.setId(id1);
		Bill bill2 = BillHelper.setup();
		int id2 = 2 ;
		bill2.setId(id2);
		BillDTO expectedBillDTO1 = getObjectMapper().map(bill1, BillDTO.class);
		BillDTO expectedBillDTO2 = getObjectMapper().map(bill2, BillDTO.class);
		
		BillPaymentsDTO billPaymentsDTO1 =  BillPaymentsDTOHelper.setup();
		BillPaymentsDTO billPaymentsDTO2 =  BillPaymentsDTOHelper.setup();
		ArrayList<BillPaymentsDTO> billsPaymentsDTOList = new ArrayList<BillPaymentsDTO>();
		billsPaymentsDTOList.add(billPaymentsDTO1);
		billsPaymentsDTOList.add(billPaymentsDTO2);
		
		ArrayList<BillPayments> billsPaymentsList = BillPaymentsDTOHelper.toModelList(billsPaymentsDTOList);

		ArrayList<Bill> billList = new ArrayList<Bill>();
		billList.add(bill1);
		billList.add(bill2);
		
		when(billManagerMock.getBills(eq(billsPaymentsList))).thenReturn(billList);
		
		this.mockMvc
			.perform(
					post(request)
					.contentType(MediaType.APPLICATION_JSON)
					.content(BillPaymentsDTOHelper.asJsonString(billsPaymentsDTOList))
					)		
			.andDo(log())
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(BillDTOHelper.asJsonString(expectedBillDTO1))))
			.andExpect(content().string(containsString(BillDTOHelper.asJsonString(expectedBillDTO2))))
			.andReturn();
	}

	@Test
	public void when_get_PendingBills_with_existent_patiend_code_then_response_List_of_BillDTO_and_OK() throws Exception {
		Integer code = 123;
		String request = "/bills/pending?patient_code={code}";
		
		Bill bill1 = BillHelper.setup();
		int id1 = 1 ;
		bill1.setId(id1);
		Bill bill2 = BillHelper.setup();
		int id2 = 2 ;
		bill2.setId(id2);
		BillDTO expectedBillDTO1 = getObjectMapper().map(bill1, BillDTO.class);
		BillDTO expectedBillDTO2 = getObjectMapper().map(bill2, BillDTO.class);
		
		ArrayList<Bill> billList = new ArrayList<Bill>();
		billList.add(bill1);
		billList.add(bill2);

	    List<BillDTO> billDTOS = billList.stream().map(b-> getObjectMapper().map(b, BillDTO.class)).collect(Collectors.toList());

		
		when(billManagerMock.getPendingBills(eq(code))).thenReturn(billList);
		
		this.mockMvc
			.perform(
					get(request, code)
					.contentType(MediaType.APPLICATION_JSON)
					)		
			.andDo(log())
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(BillDTOHelper.asJsonString(expectedBillDTO1))))
			.andExpect(content().string(containsString(BillDTOHelper.asJsonString(expectedBillDTO2))))
			.andExpect(content().string(containsString(BillDTOHelper.asJsonString(billDTOS))))
			.andReturn();
	}
	
	
	
	@Test
	public void when_post_SearchBillsByItem_with_valid_dates_and_billItemsDTO_content_and_PatientBrowserManager_getBills_returns_billList_then_OK() throws Exception {
		//@PostMapping(value = "/bills/search/by/item", produces = MediaType.APPLICATION_JSON_VALUE)
		//public ResponseEntity<List<BillDTO>> searchBills(@RequestParam(value="datefrom") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") Date dateFrom,
		//		@RequestParam(value="dateto")@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") Date dateTo,
		//		@RequestBody BillItemsDTO billItemDTO)
		
		String request = "/bills/search/by/item?datefrom={dateFrom}&dateto={dateTo}";
		String dateFrom = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());
		String dateTo = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());
		
		BillItemsDTO billItemsDTO = BillItemsDTOHelper.setup();
	    BillItems billItem = getObjectMapper().map(billItemsDTO, BillItems.class);
	             
	    ArrayList<Bill> billList = new ArrayList<Bill>();
	    Integer id = 0;
	    Bill bill = BillHelper.setup(id);
	    billList.add(bill);
		
	    //TODO add test(s) with incorrect formatted dates returning an exception
	    //TODO add test(s) with specific dates returning an empty list and asserting  HttpStatus.NO_CONTENT
	    //TODO add test(s) with different BillItem returning an empty list and asserting  HttpStatus.NO_CONTENT
	    when(billManagerMock.getBills(any(GregorianCalendar.class), any(GregorianCalendar.class), eq(billItem))).thenReturn(billList);

	    this.mockMvc
			.perform(
					post(request, dateFrom, dateTo)
					.contentType(MediaType.APPLICATION_JSON)
					.content(BillItemsDTOHelper.asJsonString(billItemsDTO))
					)		
			.andDo(log())
			.andDo(print())
			.andExpect(status().isOk())
			// TODO 1 .andExpect(content().string(containsString(BillDTOHelper.asJsonString(BillDTOHelper.setup(id)))))
			.andReturn();
		
	}
	
	
	@Test
	public void when_get_SearchBills_with_valid_dates_and_valid_patient_code_and_PatientBrowserManager_getBills_returns_billList_then_OK() throws Exception {
//		@GetMapping(value = "/bills", produces = MediaType.APPLICATION_JSON_VALUE)
//		public ResponseEntity<List<BillDTO>> searchBills(@RequestParam(value="datefrom") @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") Date dateFrom,
//				@RequestParam(value="dateto")@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") Date dateTo,
//				@RequestParam(value="patient_code", required=false, defaultValue="") Integer code) throws OHServiceException {
//	        
		
		String request = "/bills?datefrom={dateFrom}&dateto={dateTo}&patient_code={patient_code}";
		String dateFrom = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());
		String dateTo = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date());
		Integer patientCode = 1;
		
		ArrayList<Bill> billList = new ArrayList<Bill>();
	    Integer id = 0;
	    Bill bill = BillHelper.setup(id);
	    billList.add(bill);

	    Patient patient = new TestPatient().setup(true);
	    
	    when(patientManagerMock.getPatient(eq(patientCode))).thenReturn(patient);
	   
	    //TODO add test(s) with incorrect formatted dates returning an exception 
	    //TODO add test(s) with specific dates returning an empty list and asserting  HttpStatus.NO_CONTENT
	    //TODO add test(s) with a different patient returning an empty list and asserting  HttpStatus.NO_CONTENT
	    when(billManagerMock.getBills(any(GregorianCalendar.class), any(GregorianCalendar.class), eq(patient))).thenReturn(billList);
	    
	    this.mockMvc
			.perform(
					get(request, dateFrom, dateTo, patientCode)
					.contentType(MediaType.APPLICATION_JSON)
					)		
			.andDo(log())
			.andDo(print())
			.andExpect(status().isOk())
			// TODO 1 .andExpect(content().string(containsString(BillDTOHelper.asJsonString(BillDTOHelper.setup(id)))))
			.andReturn();
	
	}
	
	
	@Test
	public void when_get_getDistinctItems_BillBrowserManager_getDistinctItems_returns_BillItemsDTOList_then_OK() throws Exception {
		 
//			@GetMapping(value = "/bills/items", produces = MediaType.APPLICATION_JSON_VALUE)
//			public ResponseEntity<List<BillItemsDTO>> getDistinctItems() throws OHServiceException {
				
		String request = "/bills/items";
		
		//TODO move to a Helper once it duplicates somewhere else
		Bill bill = BillHelper.setup();
		TestBillItems tbi = new TestBillItems();
		BillItems billItems1 = tbi.setup(bill, false);
		BillItems billItems2 = tbi.setup(bill, false);
		ArrayList<BillItems> billItemsList = new ArrayList<BillItems>();
		billItemsList.add(billItems1);
		billItemsList.add(billItems2);
		
        List<BillItemsDTO> expectedBillItemsDTOList = billItemsList.stream().map(it-> getObjectMapper().map(it, BillItemsDTO.class)).collect(Collectors.toList());
        
        //TODO emulate distinct behavior since both billItems in List are equal
        when(billManagerMock.getDistinctItems()).thenReturn(billItemsList);
			    
	    this.mockMvc
			.perform(
					get(request)
					.contentType(MediaType.APPLICATION_JSON)
					)		
			.andDo(log())
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(content().string(containsString(BillItemsDTOHelper.asJsonString(expectedBillItemsDTOList))))
			.andReturn();
        
	}

	static class BillHelper{
		
		public static Bill setup() throws OHException{
			TestPatient testPatient =  new TestPatient();
			Patient patient = testPatient.setup(false); 
			TestPriceList testPriceList =  new TestPriceList();
			PriceList priceList = testPriceList.setup(false);
			TestBill testBill = new TestBill();
			Bill bill = testBill.setup(priceList, patient, false);
			//bill.setId(id);
			return bill;
		}
		
		public static Bill setup(Integer id) throws OHException {
			Bill bill = BillHelper.setup();
			bill.setId(id);
			return bill;
			
		}

		public static ArrayList<Bill> setupBillList(int size) {
			return (ArrayList<Bill>) IntStream.range(1, size+1)
					.mapToObj(i -> {	Bill b = null;
										try {
											b = BillHelper.setup();
										} catch (OHException e) {
											e.printStackTrace();
										}
										return b;
									}
					).collect(Collectors.toList());
		}
		
		public static String asJsonString(Bill bill){
			try {
				return new ObjectMapper().writeValueAsString(bill);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	static class FullBillDTOHelper{
		public static FullBillDTO setup() throws OHException{
			Bill bill = BillHelper.setup();
			FullBillDTO fullBillDTO = new  FullBillDTO();
			
			Patient patient = new TestPatient().setup(true);
			PatientDTO patientDTO = OHModelMapper.getObjectMapper().map(patient, PatientDTO.class);
			
			BillDTO billDTO = new BillDTO();
			billDTO.setPatientDTO(patientDTO);
			billDTO.setListName(bill.getListName());
			billDTO.setPatName(patient.getFirstName());

			//OP-205
			//BillDTO billDTO = OHModelMapper.getObjectMapper().map(bill, BillDTO.class);
			
			fullBillDTO.setBill(billDTO);

			
			TestBillItems tbi = new TestBillItems();
			BillItems billItems = tbi.setup(bill, false);
			BillItemsDTO billItemsDTO = OHModelMapper.getObjectMapper().map(billItems, BillItemsDTO.class);
			fullBillDTO.setBillItems(Arrays.asList(billItemsDTO));

			TestBillPayments tbp = new TestBillPayments();
			BillPayments billPayments = tbp.setup(bill, false);
			BillPaymentsDTO billPaymentsDTO = OHModelMapper.getObjectMapper().map(billPayments, BillPaymentsDTO.class);
			fullBillDTO.setBillPayments(Arrays.asList(billPaymentsDTO));

			
			return fullBillDTO;
		}
		
		public static String asJsonString(FullBillDTO fullBillDTO){
			try {
				return new ObjectMapper().writeValueAsString(fullBillDTO);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		public static String asJsonString(List<FullBillDTO> fullBillDTOList){
			try {
				return new ObjectMapper().writeValueAsString(fullBillDTOList);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	static class BillDTOHelper{
		public static BillDTO setup() throws OHException{
			Bill bill = BillHelper.setup();
			FullBillDTO fullBillDTO = new  FullBillDTO();
			
			Patient patient = new TestPatient().setup(true);
			PatientDTO patientDTO = OHModelMapper.getObjectMapper().map(patient, PatientDTO.class);
			
			BillDTO billDTO = new BillDTO();
			billDTO.setPatientDTO(patientDTO);
			fullBillDTO.setBill(billDTO);
			
		
			
			return billDTO;
		}
		
		public static BillDTO setup(Integer id) throws OHException {
			BillDTO billDTO = BillDTOHelper.setup();
			billDTO.setId(id);
			return billDTO;
		}

		public static String asJsonString(BillDTO billDTO){
			try {
				return new ObjectMapper().writeValueAsString(billDTO);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		public static String asJsonString(List<BillDTO> billDTOList){
			try {
				return new ObjectMapper().writeValueAsString(billDTOList);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

	
	static class BillPaymentsDTOHelper{
		public static BillPaymentsDTO setup() throws OHException{
			Bill bill = BillHelper.setup();

//			FullBillDTO fullBillDTO = new  FullBillDTO();
//			Patient patient = new TestPatient().setup(true);
//			PatientDTO patientDTO = OHModelMapper.getObjectMapper().map(patient, PatientDTO.class);
//			BillDTO billDTO = new BillDTO();
//			billDTO.setPatientDTO(patientDTO);
//			fullBillDTO.setBill(billDTO);
			
			TestBillPayments testBillPayments =  new TestBillPayments();
			
			BillPayments billPayments = testBillPayments.setup(bill, true);
		
			BillPaymentsDTO billPaymentsDTO = OHModelMapper.getObjectMapper().map(billPayments, BillPaymentsDTO.class);
			return billPaymentsDTO;
		}
		
		public static String asJsonString(BillPaymentsDTO billPaymentsDTO){
			try {
				return new ObjectMapper().writeValueAsString(billPaymentsDTO);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		public static String asJsonString(List<BillPaymentsDTO> billPaymentsDTOList){
			try {
				return new ObjectMapper().writeValueAsString(billPaymentsDTOList);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		static ArrayList<BillPayments> toModelList(List<BillPaymentsDTO> billPaymentsDTOList){
	        ArrayList<BillPayments> billPayments = new ArrayList<BillPayments>(billPaymentsDTOList.stream().map(pay-> getObjectMapper().map(pay, BillPayments.class)).collect(Collectors.toList()));
	        return billPayments;
		}
	}
	
	
	static class BillItemsDTOHelper{
		public static BillItemsDTO setup() throws OHException{
			Bill bill = BillHelper.setup();
			TestBillItems tbi = new TestBillItems();
			BillItems billItems = tbi.setup(bill, false);
			BillItemsDTO billItemsDTO = OHModelMapper.getObjectMapper().map(billItems, BillItemsDTO.class);
			return billItemsDTO;
		}
		
		public static String asJsonString(BillItemsDTO billItemsDTO){
			try {
				return new ObjectMapper().writeValueAsString(billItemsDTO);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		public static String asJsonString(List<BillItemsDTO> billItemsDTOList){
			try {
				return new ObjectMapper().writeValueAsString(billItemsDTOList);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		static ArrayList<BillItems> toModelList(List<BillItemsDTO> billItemsDTOList){
	        ArrayList<BillItems> billItems = new ArrayList<BillItems>(billItemsDTOList.stream().map(pay-> getObjectMapper().map(pay, BillItems.class)).collect(Collectors.toList()));
	        return billItems;
		}
	}
	
	//TODO
	
	@Test
	public void zzzzz_testGetPaymentsByBillId() {
		fail("Not yet implemented");
	}


	
}
