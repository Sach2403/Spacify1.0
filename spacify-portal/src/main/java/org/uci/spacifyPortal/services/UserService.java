package org.uci.spacifyPortal.services;

import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.uci.spacifyLib.entity.*;
import org.uci.spacifyLib.repository.AuthenticationRepository;
import org.uci.spacifyLib.repository.MacAddressRepository;
import org.uci.spacifyLib.repository.SubscriberRepository;
import org.uci.spacifyLib.repository.UserRepository;

import javax.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MacAddressRepository macAddressRepository;

    @Autowired
    private AuthenticationRepository authenticationRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    public List<UserEntity> getAllEligibleOwners() {
        return this.userRepository.findByAccessLevelIn(Arrays.asList(AccessLevel.ADMIN, AccessLevel.FACULTY));
    }

    public void saveUserEntity(UserEntity userEntity) {
        this.userRepository.save(userEntity);
    }

    public Optional<UserEntity> getUser(String userId) {
        return this.userRepository.findById(userId);
    }

    public List<String> getMacAddressesForUser(String userId) {
        List<MacAddressEntity> macAddressEntityList = this.macAddressRepository.findAllByUserId(userId);
        if (macAddressEntityList.isEmpty())
            return new ArrayList<String>();

        return macAddressEntityList.stream().map(MacAddressEntity::getMacAddress).collect(Collectors.toList());
    }

    public boolean updateMacAddresses(String userId, List<String> macAddresses) {
        // remove duplicate mac addresses
        Set<String> uniqueMacAddresses = new HashSet<>(macAddresses);
        // check whether provided mac address is assigned to another user
        List<MacAddressEntity> macAddressEntityList = this.macAddressRepository.findAllById(uniqueMacAddresses);
        Optional<MacAddressEntity> optionalMacAddressEntity = macAddressEntityList.stream().filter(macAddressEntity -> !macAddressEntity.getUserId().equals(userId)).findFirst();
        if (optionalMacAddressEntity.isPresent()) {
            return false;
        }
        // delete existing mac addresses for user
        this.macAddressRepository.deleteAll(this.macAddressRepository.findAllByUserId(userId));

        // add new mac addresses for user
        List<MacAddressEntity> updatedMacAddress = uniqueMacAddresses.stream().map(macAddress -> new MacAddressEntity(userId, macAddress)).collect(Collectors.toList());
        this.macAddressRepository.saveAll(updatedMacAddress);
        return true;
    }

    public Optional<UserEntity> checkIfUserExists(String userId, String email) {
        if (Objects.isNull(userId) || Objects.isNull(email))
            return Optional.empty();
        Optional<UserEntity> userEntity = this.userRepository.findById(userId);
        if (userEntity.isPresent())
            return userEntity;
        userEntity = this.userRepository.findByEmail(email);
        return userEntity;
    }

    public String hashPassword(String password) {
        String hashedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashInBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            hashedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hashedPassword;
    }

    public String generateRandomCode() {
        List<CharacterRule> rules = List.of(new CharacterRule(EnglishCharacterData.Alphabetical, 20));

        PasswordGenerator generator = new PasswordGenerator();
        return generator.generatePassword(20, rules);
    }

    public void saveNewPassword(AuthenticationEntity authenticationEntity) {
        this.authenticationRepository.save(authenticationEntity);
    }

    @Transactional(rollbackOn = {SQLException.class})
    protected void saveRecords(UserEntity userEntity, MacAddressEntity macAddressEntity, AuthenticationEntity authenticationEntity) throws SQLException {
        this.userRepository.save(userEntity);
        this.macAddressRepository.save(macAddressEntity);
        this.authenticationRepository.save(authenticationEntity);
    }

    public String createAndSaveNewUser(String userId, String emailId, String password, String firstName, String lastName, String macAddress) {
        String hashedPassword = this.hashPassword(password);
        if (Objects.isNull(hashedPassword)) {
            return null;
        }
        String verificationCode = this.generateRandomCode();
        UserEntity userEntity = new UserEntity(userId, emailId, firstName, lastName, AccessLevel.ADMIN, 0L, verificationCode, false, null);

        try {
            this.saveRecords(userEntity, new MacAddressEntity(userId, macAddress), new AuthenticationEntity(userId, hashedPassword));
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return verificationCode;
    }

    public Optional<AuthenticationEntity> getAuthentication(String userId) {
        return this.authenticationRepository.findById(userId);
    }

    public boolean verifyUserSignIn(AuthenticationEntity authenticationEntity, String password) {
        String hashedPassword = this.hashPassword(password);
        if (Objects.isNull(hashedPassword)) {
            return false;
        }
        return Objects.equals(authenticationEntity.getHashedPassword(), hashedPassword);
    }

    public void updateUserProfForWhatsappSub(String userId, String phoneNumber, String roomId) {

        Optional<UserEntity> profile = userRepository.findById(userId);
        profile.ifPresent(userEntity -> {
            userEntity.setPhoneNumber(phoneNumber);
            userRepository.save(profile.get());
        });


        UserRoomPK pk = new UserRoomPK(userId, Long.parseLong(roomId));
        Optional<SubscriberEntity> subscriberEntity = subscriberRepository.findAllByUserRoomPK(pk);

        profile.ifPresent(subUser -> {
            subscriberEntity.get().setSubscribed(true);
            subscriberRepository.save(subscriberEntity.get());
        });


    }

}
