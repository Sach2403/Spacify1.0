package org.uci.spacifyPortal.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.uci.spacifyLib.dto.RoomDetail;
import org.uci.spacifyLib.dto.SubsRequest;
import org.uci.spacifyLib.entity.*;
import org.uci.spacifyLib.services.SubscriberService;
import org.uci.spacifyPortal.services.*;
import org.uci.spacifyPortal.utilities.ChangePasswordRequest;
import org.uci.spacifyPortal.utilities.MessageResponse;
import org.uci.spacifyPortal.utilities.RegisterUserRequest;
import org.uci.spacifyPortal.utilities.UserDetail;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(maxAge = 3600)
@RestController
@RequestMapping("api/v1/user")
public class UserController {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private UserService userService;

    @Autowired
    private IncentiveService incentiveService;

    @Autowired
    private SubscriberService subscriberService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ReservationService reservationService;

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/eligibleOwners")
    public List<UserEntity> getAllEligibleOwners() {
        return this.userService.getAllEligibleOwners();
    }

    private List<RoomDetail> getOwnedRoomDetails(String userId) {
        List<OwnerEntity> ownerEntities = this.ownerService.getAllOwnedRooms(userId);
        List<RoomDetail> ownedRooms = new ArrayList<RoomDetail>();
        if (!ownerEntities.isEmpty()) {
            List<Long> ownerRoomIds = ownerEntities
                    .stream()
                    .map(e -> e.getUserRoomPK().getRoomId())
                    .collect(Collectors.toList());
            ownedRooms = this.roomService.getRoomsBasedOnIds(ownerRoomIds)
                    .stream()
                    .map(room -> new RoomDetail(room.getRoomId(), room.getRoomName(), room.getRoomType().getPrettyName()))
                    .collect(Collectors.toList());
        }

        return ownedRooms;
    }

    @GetMapping("/userSubscribedRooms/{userId}")
    public List<RoomDetail> getSubscribedRoomDetails(@PathVariable String userId) {
        List<SubscriberEntity> subscriberEntities = this.subscriberService.getAllSubscribedRooms(userId);
        List<RoomDetail> subscribedRooms = new ArrayList<RoomDetail>();
        if (!subscriberEntities.isEmpty()) {
            List<Long> subscribedRoomIds = subscriberEntities
                    .stream()
                    .map(e -> e.getUserRoomPK().getRoomId())
                    .collect(Collectors.toList());
            subscribedRooms = this.roomService.getRoomsBasedOnIds(subscribedRoomIds)
                    .stream()
                    .map(room -> new RoomDetail(room.getRoomId(), room.getRoomName(), room.getRoomType().getPrettyName()))
                    .collect(Collectors.toList());
        }
        return subscribedRooms;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetail> getUserDetails(@PathVariable String userId) {
        UserDetail userDetail = new UserDetail();
        Optional<UserEntity> userEntity = this.userService.getUser(userId);
        if (userEntity.isPresent()) {
            userDetail.setUser(userEntity.get());
            List<IncentiveEntity> incentiveEntities = this.incentiveService.getIncentivesForUser(userId);
            userDetail.setIncentives(incentiveEntities);
            userDetail.setOwnedRooms(getOwnedRoomDetails(userId));
            userDetail.setSubscribedRooms(getSubscribedRoomDetails(userId));
            userDetail.setTotalIncentive(userDetail.getTotalIncentive());
            userDetail.setMacAddresses(this.userService.getMacAddressesForUser(userId));
            return new ResponseEntity<>(userDetail, HttpStatus.OK);
        }

        return new ResponseEntity<>(userDetail, HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/updateUser")
    public ResponseEntity<MessageResponse> updateUserProfile(@RequestBody UserDetail userDetail) {
        if (Objects.isNull(userDetail.getUser()) || Objects.isNull(userDetail.getUser().getUserId())) {
            MessageResponse messageResponse = new MessageResponse("Invalid request, please provide user name.", false);
            return new ResponseEntity<MessageResponse>(messageResponse, HttpStatus.BAD_REQUEST);
        }

        Optional<UserEntity> userEntityOptional = this.userService.getUser(userDetail.getUser().getUserId());
        if (userEntityOptional.isPresent()) {
            UserEntity userEntity = userEntityOptional.get();
            if (Objects.nonNull(userDetail.getUser().getFirstName())) {
                userEntity.setFirstName(userDetail.getUser().getFirstName());
            }
            if (Objects.nonNull(userDetail.getUser().getLastName())) {
                userEntity.setLastName(userDetail.getUser().getLastName());
            }
            if (Objects.nonNull(userDetail.getMacAddresses()) && !userDetail.getMacAddresses().isEmpty()) {
                if (!this.userService.updateMacAddresses(userDetail.getUser().getUserId(), userDetail.getMacAddresses())) {
                    MessageResponse messageResponse = new MessageResponse("Mac Address registered to another user. Please enter your own device's Mac Address.", false);
                    return new ResponseEntity<MessageResponse>(messageResponse, HttpStatus.OK);
                }
            }
            this.userService.saveUserEntity(userEntity);
            MessageResponse messageResponse = new MessageResponse("Your profile has been successfully updated!", true);
            return new ResponseEntity<MessageResponse>(messageResponse, HttpStatus.OK);
        } else {
            MessageResponse messageResponse = new MessageResponse("No such user exists.", false);
            return new ResponseEntity<MessageResponse>(messageResponse, HttpStatus.BAD_REQUEST);
        }
    }

    // close account functionality. Erases user from the system.
//    @DeleteMapping("/{userId}")
//    public ResponseEntity<MessageResponse> deleteUser(@PathVariable String userId) {
//        if (Objects.isNull(userId) || userId.isEmpty()) {
//            return new ResponseEntity<MessageResponse>(new MessageResponse("Please provide valid user name.", false), HttpStatus.BAD_REQUEST);
//        }
//
//        Optional<UserEntity> userEntity = this.userService.getUser(userId);
//        if (userEntity.isPresent()) {
//            this.userService.deleteUser(userId);
//            this.incentiveService.deleteIncentivesForUser(userId);
//            this.subscriberService.deleteSubscriptionsForUser(userId);
//
//            List<Integer> roomIds = this.ownerService.deleteOwnersForUser(userId);
//            this.roomService.deleteRoomsCreatedByUser(roomIds);
//
//            this.reservationService.deleteReservationsForUser(userId);
//
//        } else {
//            return new ResponseEntity<MessageResponse>(new MessageResponse("No such user name exists.", false), HttpStatus.BAD_REQUEST);
//        }
//    }
//
    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> registerNewUser(@RequestBody RegisterUserRequest registerUserRequest) {
        Optional<UserEntity> userEntity = this.userService.checkIfUserExists(registerUserRequest.getUserId(), registerUserRequest.getEmailId());
        if (userEntity.isPresent()) {
            String message = "Email already registered. Please Sign In.";
            if (userEntity.get().getUserId().equals(registerUserRequest.getUserId())) {
                message = "User name already exists, please choose another user name";
            }
            MessageResponse messageResponse = new MessageResponse(message, false);
            return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
        }
        String verificationCode = this.userService.createAndSaveNewUser(registerUserRequest.getUserId(),
                registerUserRequest.getEmailId(),
                registerUserRequest.getPassword(),
                registerUserRequest.getFirstName(),
                registerUserRequest.getLastName(),
                registerUserRequest.getMacAddress());

        if (Objects.isNull(verificationCode)) {
            MessageResponse messageResponse = new MessageResponse("Unable to Register. Please try again later.", false);
            return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
        }

        this.emailService.sendSimpleMessageForVerification(registerUserRequest.getEmailId(), registerUserRequest.getUserId(), verificationCode);
        MessageResponse messageResponse = new MessageResponse("User successfully registered! Please click the verification link sent to your email.", true);
        return new ResponseEntity<>(messageResponse, HttpStatus.OK);
    }

    @PostMapping("/signin")
    public ResponseEntity<MessageResponse> userSignIn(@RequestBody RegisterUserRequest registerUserRequest) {
        String userId = registerUserRequest.getUserId();
        Optional<AuthenticationEntity> authenticationEntity = this.userService.getAuthentication(userId);
        if (authenticationEntity.isPresent()) {
            Optional<UserEntity> userEntity = this.userService.getUser(userId);
            if (userEntity.isPresent() && userEntity.get().getVerified()) {
                boolean signInAllowed = this.userService.verifyUserSignIn(authenticationEntity.get(), registerUserRequest.getPassword());
                if (signInAllowed) {
                    MessageResponse messageResponse = new MessageResponse("Successfully signed in!", true);
                    return new ResponseEntity<>(messageResponse, HttpStatus.OK);
                } else {
                    MessageResponse messageResponse = new MessageResponse("Invalid credentials. Please try again.", false);
                    return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
                }
            }
            MessageResponse messageResponse = new MessageResponse("User not verified. Please verify yourself by clicking the verification link in your inbox, or click Send Verification button for a new link. ", false);
            return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
        }

        MessageResponse messageResponse = new MessageResponse("User name does not exist. Please register if you are a new user.", false);
        return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/verification/{userId}/{code}")
    public String verifyUser(@PathVariable String userId, @PathVariable String code) {
        Optional<UserEntity> userEntity = this.userService.getUser(userId);
        if (userEntity.isPresent()) {
            UserEntity user = userEntity.get();
            if (user.getVerified()) {
                return "Your email has already been verified! Please Sign In.";
            }

            if (Objects.equals(user.getVerificationCode(), code)) {
                user.setVerified(true);
                this.userService.saveUserEntity(user);
                return "Yay! Your email has been verified. You can now Sign In and use Spacify!";
            }

            return "Oops! You have an invalid verification link. Contact support.";
        }

        return "Invalid user. Please Sign Up to use Spacify!";
    }

    @GetMapping("/sendVerification/{userId}")
    public ResponseEntity<MessageResponse> resendVerificationLink(@PathVariable String userId) {
        Optional<UserEntity> userEntity = this.userService.getUser(userId);
        if (userEntity.isPresent()) {
            UserEntity user = userEntity.get();
            if (user.getVerified()) {
                MessageResponse messageResponse = new MessageResponse("Your email has already been verified! Please Sign In.", true);
                return new ResponseEntity<>(messageResponse, HttpStatus.OK);
            }

            if (Objects.isNull(user.getVerificationCode())) {
                user.setVerificationCode(this.userService.generateRandomCode());
                this.userService.saveUserEntity(user);
            }
            this.emailService.sendSimpleMessageForVerification(user.getEmail(), user.getUserId(), user.getVerificationCode());
            MessageResponse messageResponse = new MessageResponse("Verification email sent!", true);
            return new ResponseEntity<>(messageResponse, HttpStatus.OK);
        }

        MessageResponse messageResponse = new MessageResponse("Invalid user. Please Sign Up to use Spacify!", false);
        return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
    }

    @GetMapping("/forgotPassword/{userId}")
    public ResponseEntity<MessageResponse> forgotPasswordMail(@PathVariable String userId) {
        Optional<UserEntity> userEntity = this.userService.getUser(userId);
        if (userEntity.isPresent()) {
            UserEntity user = userEntity.get();
            String newPassword = this.userService.generateRandomCode();

            Optional<AuthenticationEntity> authenticationEntity = this.userService.getAuthentication(userId);
            // this helps in back filling the users which were created before this feature.
            if (authenticationEntity.isPresent()) {
                authenticationEntity.get().setHashedPassword(this.userService.hashPassword(newPassword));
                this.userService.saveNewPassword(authenticationEntity.get());
            } else {
                this.userService.saveNewPassword(new AuthenticationEntity(userId, this.userService.hashPassword(newPassword)));
            }

            this.emailService.sendNewPassword(user.getEmail(), user.getFirstName(), newPassword);
            MessageResponse messageResponse = new MessageResponse("We have sent the new password to your verified email!", true);
            return new ResponseEntity<>(messageResponse, HttpStatus.OK);
        }

        MessageResponse messageResponse = new MessageResponse("Invalid user. Please Sign Up to use Spacify!", false);
        return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/updateSubscriber")
    public ResponseEntity<MessageResponse> updateSubscriber(@RequestBody SubsRequest subsRequest) {

        try {
            LOG.info("Subscribing on whatsapp");
            userService.updateUserProfForWhatsappSub(subsRequest.getUserId(), subsRequest.getPhoneNumber(), subsRequest.getRoomId());
            MessageResponse messageResponse = new MessageResponse("Voila! you have successfully connected with us on whatsapp.", true);
            return new ResponseEntity<>(messageResponse, HttpStatus.OK);

        }catch(Exception e){

            LOG.error("Error while subscribing for whatsapp : {}", e.getMessage(),e);
            MessageResponse messageResponse = new MessageResponse("Error while subscribing to whatsapp. Please check with the admin", false);
            return new ResponseEntity<>(messageResponse, HttpStatus.INTERNAL_SERVER_ERROR);

        }

    }


    @PostMapping("/changePassword")
    public ResponseEntity<MessageResponse> userSignIn(@RequestBody ChangePasswordRequest changeRequest) {
        Optional<AuthenticationEntity> authenticationEntity = this.userService.getAuthentication(changeRequest.getUserId());
        if (authenticationEntity.isPresent()) {
            boolean signInAllowed = this.userService.verifyUserSignIn(authenticationEntity.get(), changeRequest.getOldPassword());
            if (signInAllowed) {
                authenticationEntity.get().setHashedPassword(this.userService.hashPassword(changeRequest.getNewPassword()));
                this.userService.saveNewPassword(authenticationEntity.get());
                MessageResponse messageResponse = new MessageResponse("Your password was changed successfully!.", true);
                return new ResponseEntity<>(messageResponse, HttpStatus.OK);
            } else {
                MessageResponse messageResponse = new MessageResponse("Your old password is incorrect! Please enter correct password.", false);
                return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
            }
        }

        MessageResponse messageResponse = new MessageResponse("User name does not exist. Please register if you are a new user.", false);
        return new ResponseEntity<>(messageResponse, HttpStatus.BAD_REQUEST);
    }
}
