package gov.meghalaya.meghaconnect.service;

import gov.meghalaya.meghaconnect.entity.Person;
import gov.meghalaya.meghaconnect.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonService {

    private final PersonRepository personRepository;

    public Optional<Person> findByPhone(String phone) {
        return personRepository.findByPhoneNumber(phone);
    }

    public Optional<Person> findByEpic(String epic) {
        return personRepository.findByEpicNumber(epic);
    }

    public List<Person> searchByName(String name) {
        return personRepository.searchByName(name);
    }

    public List<Person> findByConstituency(String constituency) {
        return personRepository.findByConstituency(constituency);
    }

    @Transactional
    public Person save(Person person) {
        return personRepository.save(person);
    }

    public Optional<Person> findById(Long id) {
        return personRepository.findById(id);
    }
}
