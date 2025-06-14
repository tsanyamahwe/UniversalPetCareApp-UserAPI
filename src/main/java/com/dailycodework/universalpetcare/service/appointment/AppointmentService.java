package com.dailycodework.universalpetcare.service.appointment;

import com.dailycodework.universalpetcare.enums.AppointmentStatus;
import com.dailycodework.universalpetcare.exception.ResourceNotFoundException;
import com.dailycodework.universalpetcare.model.Appointment;
import com.dailycodework.universalpetcare.model.Pet;
import com.dailycodework.universalpetcare.model.User;
import com.dailycodework.universalpetcare.repository.AppointmentRepository;
import com.dailycodework.universalpetcare.repository.PetRepository;
import com.dailycodework.universalpetcare.repository.UserRepository;
import com.dailycodework.universalpetcare.request.AppointmentUpdateRequest;
import com.dailycodework.universalpetcare.request.BookAppointmentRequest;
import com.dailycodework.universalpetcare.service.pet.IPetService;
import com.dailycodework.universalpetcare.utils.FeedBackMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AppointmentService implements IAppointmentService{
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final IPetService petService;

    @Transactional
    @Override
    public Appointment createAppointment(BookAppointmentRequest bookAppointmentRequest, Long senderId, Long recipientId) {
        Optional<User> sender = userRepository.findById(senderId);
        Optional<User> recipient = userRepository.findById(recipientId);
        if(sender.isPresent() && recipient.isPresent()){
            Appointment appointment = bookAppointmentRequest.getAppointment();
            List<Pet> pets = bookAppointmentRequest.getPet();
            pets.forEach(pet -> pet.setAppointment(appointment));
            List<Pet> savedPet = petService.savePetForAppointment(pets);
            appointment.setPets(savedPet);
            appointment.addPatient(sender.get());
            appointment.addVeterinarian(recipient.get());
            appointment.setAppointmentNo();
            appointment.setStatus(AppointmentStatus.WAITING_FOR_APPROVAL);
            return appointmentRepository.save(appointment);
        }
        throw new ResourceNotFoundException(FeedBackMessage.SENDER_RECIPIENT_NOT_FOUND);
    }

    @Override
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    @Override
    public Appointment updateAppointment(Long id, AppointmentUpdateRequest appointmentUpdateRequestRequest) {
        Appointment existingAppointment = getAppointmentById(id);
        if(!Objects.equals(existingAppointment.getStatus(), AppointmentStatus.WAITING_FOR_APPROVAL)){
            throw new IllegalStateException(FeedBackMessage.ALREADY_APPROVED);
        }else{
            existingAppointment.setAppointmentDate(appointmentUpdateRequestRequest.getAppointmentDate());
            existingAppointment.setAppointmentTime(appointmentUpdateRequestRequest.getAppointmentTime());
            existingAppointment.setReason(appointmentUpdateRequestRequest.getReason());
        }
        return appointmentRepository.save(existingAppointment);
    }

    @Override
    public void deleteAppointmentById(Long id) {
        appointmentRepository.findById(id).ifPresentOrElse(appointmentRepository::delete, ()->{throw new ResourceNotFoundException(FeedBackMessage.NOT_FOUND);});
    }

    @Override
    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id).orElseThrow(()->new ResourceNotFoundException(FeedBackMessage.NOT_FOUND));
    }

    @Override
    public Appointment getAppointmentByNo(String appointmentNo) {
        return appointmentRepository.findByAppointmentNo(appointmentNo);
    }
//
//    @Override
//    public Appointment getAppointmentByDate(Date appointmentDate) {
//        return appointmentRepository.findByAppointmentDate(appointmentDate);
//    }
//
//    @Override
//    public Appointment getAppointmentByTime(Time appointmentTime) {
//        return appointmentRepository.findAppointmentByTime(appointmentTime);
//    }
//
//    @Override
//    public Appointment getAppointmentByAllSpecs(Long id, String appointmentNo, Date appointmentDate, Time appointmentTime) {
//        return appointmentRepository.findAppointmentByAllSpecs(id, appointmentNo, appointmentDate, appointmentTime);
//    }
}
