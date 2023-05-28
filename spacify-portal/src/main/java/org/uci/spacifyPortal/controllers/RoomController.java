package org.uci.spacifyPortal.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uci.spacifyLib.dto.*;
import org.uci.spacifyLib.entity.RoomEntity;
import org.uci.spacifyLib.services.TippersConnectivityService;
import org.uci.spacifyPortal.services.OwnerService;
import org.uci.spacifyPortal.services.RoomService;
import org.uci.spacifyPortal.services.SubscriberService;
import org.uci.spacifyPortal.utilities.MessageResponse;
import org.uci.spacifyPortal.utilities.TipperSpace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("/api/v1/room")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @Autowired
    private TippersConnectivityService tippersConnectivityService;

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private SubscriberService subscriberService;

    private static final Logger LOG = LoggerFactory.getLogger(RoomController.class);

    @PostMapping("/room")
    public ResponseEntity<String> createRoom(@RequestBody RulesRequest request) throws Exception {
        Long roomId = request.getRoomId();
        String owner = request.getUserId();
        List<Rule> rules = request.getRules();

        // Call the create room service with the provided data
        roomService.createRoom(roomId, owner, rules);

        return new ResponseEntity<>("Room created successfully", HttpStatus.CREATED);
    }


    // only for testing
    @PostMapping("/test")
    @ResponseBody
    public String getAllBuildings(@RequestBody String hubVerifyToken) {

        if (hubVerifyToken.contains("button_reply")) {
            System.out.println(hubVerifyToken);
        }
//        tippersConnectivityService.getOccupancyStatusForSpaceId("1606", "2023-03-06 21:00:51.506", "2023-03-06 21:25:51.506");
//        return tippersConnectivityService.getMacAddressesForSpaceId("1606", "2023-03-06 21:00:51.506", "2023-03-06 21:25:51.506").get();
//        return tippersConnectivityService.getListOfBuildings();
//        return tippersConnectivityService.getSpaceIdAndRoomName(1605);

        return hubVerifyToken;
    }

    //only for testing
    @GetMapping("/verifyWebhook")
    @ResponseBody
    public String verifyWhatsappWebhook(@RequestParam("hub.mode") String hubMode, @RequestParam("hub.challenge") String hubChallenge, @RequestParam("hub.verify_token") String hubVerifyToken) {

        System.out.println(hubVerifyToken);

        return hubChallenge;
    }

    /*
   POST API for adding new room and owner
   */
    @PostMapping("/create")
    public ResponseEntity<MessageResponse> createRoom(@RequestBody CreateRequest createRequest) {
        try {
            boolean roomExists = this.roomService.doesRoomExist(createRequest.getTippersSpaceId());
            if (roomExists) {
                return new ResponseEntity<>(new MessageResponse("Room is already owned. Please reach out to owner for access.", false), HttpStatus.IM_USED);
            }
            RoomEntity roomEntity = this.roomService.createRoom(createRequest);
            this.ownerService.createOwner(createRequest.getUserId(), roomEntity.getRoomId());
            return new ResponseEntity<>(new MessageResponse("Your room was successfully created!", true), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(new MessageResponse("Not able to create room. Try again later.", false), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/all")
    public List<RoomEntity> getAllRooms() {
        return this.roomService.getAllRooms();
    }


    //mock tippers api
    @GetMapping("/tippers/all")
    public List<TipperSpace> getAll() {
        List<TipperSpace> list = new ArrayList<TipperSpace>();
        list.add(new TipperSpace("B501", 400));
        list.add(new TipperSpace("A102", 49));
        list.add(new TipperSpace("E100", 46));
        list.add(new TipperSpace("E200", 44));
        list.add(new TipperSpace("B301", 43));

        return list;
    }

    @PostMapping("/addRules")
    public ResponseEntity<String> addRules(@RequestBody RulesRequest request) throws Exception {
        // Call the rule service with the provided data
        this.roomService.addRules(request.getRoomId(), request.getUserId(), request.getRules());

        return new ResponseEntity<>("Rules created successfully", HttpStatus.CREATED);
    }

    @GetMapping("/buildings")
    public List<RoomDetail> getBuildings() {
        return tippersConnectivityService.getListOfBuildings();
    }

    @GetMapping("/rooms/{spaceId}")
    public List<RoomDetail> getRoomsFromBuildingSpaceId(@PathVariable String spaceId) {
        return tippersConnectivityService.getSpaceIdAndRoomName(Integer.parseInt(spaceId));
    }

    @GetMapping("/subsRooms/{spaceId}")
    public List<RoomDetail> getRoomsToSubscribeFromBuildingSpaceId(@PathVariable String spaceId) {
        LOG.info("Finding rooms available for subscribing for building id : {}", spaceId);
        return roomService.getRoomsForSubscribtion(spaceId);
    }

    @GetMapping("/roomRules/{spaceId}")
    public List<String> getRoomRules(@PathVariable String spaceId) throws IOException {
        LOG.info("Finding rules for the room {}", spaceId);
        return roomService.getRoomRules(spaceId);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<MessageResponse> subscribe(@RequestBody StringPairDto requestBody) {
        LOG.info("subscribing");

        try {
            boolean success = roomService.subscribeToRoom(requestBody.getString1(), requestBody.getString2());
            if (success) {
                return new ResponseEntity<>(new MessageResponse("You have successfully subscribed to the room", true), HttpStatus.OK);

            } else {
                return new ResponseEntity<>(new MessageResponse("You have already subscribed to this room", false), HttpStatus.PRECONDITION_FAILED);
            }
        } catch (Exception e) {
            LOG.error("Error while subscribing with error message: {}", e.getMessage(), e);
            return new ResponseEntity<>(new MessageResponse("Error while subscribing. Please contact the admin.", false), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/updateSubscriberStatus")
    public ResponseEntity<MessageResponse> updateSubscriberStatus(@RequestBody UnsubsRequest unsubsRequest) {

        try {
            LOG.info("Unsubscribing from whatsapp");
            boolean success = subscriberService.updateUserSubscribedStatus(unsubsRequest.getUserId(), unsubsRequest.getRoomId());
            if(success) {
                return new ResponseEntity<>(new MessageResponse("You have successfully unsubscribed to whatsapp", true), HttpStatus.OK);
            }
            else {
                return new ResponseEntity<>(new MessageResponse("You have already unsubscribed to whatsapp", false), HttpStatus.PRECONDITION_FAILED);
            }
        }catch(Exception e){

            LOG.error("Error while unsubscribing for whatsapp : {}", e.getMessage(),e);
            MessageResponse messageResponse = new MessageResponse("Error while unsubscribing to whatsapp. Please check with the admin", false);
            return new ResponseEntity<>(messageResponse, HttpStatus.INTERNAL_SERVER_ERROR);

        }

    }

}
