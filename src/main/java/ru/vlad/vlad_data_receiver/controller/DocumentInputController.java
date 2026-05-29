package ru.vlad.vlad_data_receiver.controller;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import ru.vlad.vlad_data_receiver.filter.NamespaceFilter;
import ru.vlad.vlad_data_receiver.model.DocumentInputRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.io.StringReader;

@RestController
@RequestMapping("/api/v1")
public class DocumentInputController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentInputController.class);

    private final JAXBContext jaxbContext;

    public DocumentInputController(JAXBContext jaxbContext) {
        this.jaxbContext = jaxbContext;
    }

    @PostMapping(value = "/incoming_message",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> receiveIncomingMessage(
            @RequestBody String xmlRequest,
            HttpServletRequest servletRequest) {

        logger.info("Received request from: {}", servletRequest.getRemoteAddr());
        logger.debug("Request XML: {}", xmlRequest);

        try {
            DocumentInputRequest request = parseXml(xmlRequest);

            logger.info("Successfully parsed Document_Input_Request");
            logger.info("ID: {}", request.getID());
            logger.info("TimeStamp: {}", request.getTimeStamp());
            logger.info("blockNum: {}", request.getBlockNum());
            logger.info("totalDocs: {}", request.getTotalDocs());
            logger.info("dataSet: {}", request.getDataSet());
            logger.info("OdDocType: {}", request.getOdDocType());
            logger.info("Documents count: {}", request.getDocument().size());

            return ResponseEntity.ok("<Response><Status>SUCCESS</Status><Message>Request processed successfully</Message></Response>");
        } catch (JAXBException e) {
            logger.error("Error parsing XML: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(
                    "<Error><Status>PARSE_ERROR</Status><Message>Invalid XML format: " +
                            e.getMessage() + "</Message></Error>"
            );
        } catch (Exception e) {
            logger.error("Error processing request: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                    "<Error><Status>INTERNAL_ERROR</Status><Message>Internal server error: " +
                            e.getMessage() + "</Message></Error>"
            );
        }
    }

    private DocumentInputRequest parseXml(String xml) throws JAXBException, SAXException, IOException {
        InputSource source = new InputSource(new StringReader(xml));
        XMLReader xmlReader = XMLReaderFactory.createXMLReader();
        NamespaceFilter filter = new NamespaceFilter("http://ru.vlad/documents", true);  // ← Убираем namespace
        filter.setParent(xmlReader);
        SAXSource saxSource = new SAXSource(filter, source);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (DocumentInputRequest) unmarshaller.unmarshal(saxSource);
    }
}