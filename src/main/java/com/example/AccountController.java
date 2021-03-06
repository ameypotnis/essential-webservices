package com.example;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.lang.Integer.min;
import static java.util.stream.Collectors.toMap;

/**
 * Created by amey on 7/7/16.
 */
@Controller
@RequestMapping(value = "/api/accounts")
public class AccountController {

    private static List<Account> db = new ArrayList<>();
    private AtomicInteger idGenerator = new AtomicInteger();

    @RequestMapping(method = RequestMethod.GET)
    @ResponseBody
    public List<Account> findAll(HttpServletRequest request) {
        String page = request.getParameter("p");
        if (!StringUtils.isEmpty(page)) {
            return partition(db, 2).get(Integer.parseInt(page));
        }
        return db;
    }

    @RequestMapping(value = "/{accountNumber}", method = RequestMethod.GET)
    @ResponseBody
    public Account findOne(@PathVariable("accountNumber") Integer accountNumber) {
        return Preconditions.checkFound(findAccount(accountNumber));
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public ResponseEntity<Account> create(@RequestBody Account account) {
        Preconditions.checkNotNull(account.amount, "Amount");
        account.accountNumber = idGenerator.getAndIncrement();
        db.add(account);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{accountNumber}")
                .buildAndExpand(account.accountNumber).toUri());
        return new ResponseEntity<Account>(account, httpHeaders, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{accountNumber}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    public void update(@PathVariable("accountNumber") Integer accountNumber, @RequestBody Account account) {
        Preconditions.checkNotNull(findAccount(accountNumber), "Account");
        db.stream().forEach(acc -> {
            if(accountNumber == acc.accountNumber) {
                acc.amount = account.amount;
            }
        });
    }

    @RequestMapping(value = "/{accountNumber}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.OK)
    public void delete(@PathVariable("accountNumber") Integer accountNumber) {
        db = db.stream()
                .filter(account -> account.accountNumber != accountNumber)
                .collect(Collectors.toList());
    }

    private Account findAccount(Integer accountNumber) {
        return db.stream()
                .filter(account -> account.accountNumber.equals(accountNumber))
                .findFirst()
                .orElse(null);
    }

    private static Map<Integer, List<Account>> partition(List<Account> list, int pageSize) {
        return Stream.iterate(0, i -> i + pageSize)
                .limit((list.size() + pageSize - 1) / pageSize)
                .collect(toMap(i -> i / pageSize,
                        i -> list.subList(i, min(i + pageSize, list.size()))));
    }
}

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
class Account implements Serializable {
    Integer accountNumber;
    BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public Integer getAccountNumber() {
        return accountNumber;
    }
}

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class BadRequestException extends RuntimeException {
    public BadRequestException(String field) {
        super(field);
    }
}

@ResponseStatus(value = HttpStatus.NOT_FOUND)
class ResourceNotFoundException extends RuntimeException {
    ResourceNotFoundException() {
        super("resource not found");
    }
}

class Preconditions {
    public static <T> T checkNotNull(final T resource, String field) {
        if (resource == null) {
            throw new BadRequestException(String.format("%s should not be null", field));
        }
        return resource;
    }

    public static <T> T checkFound(final T resource) {
        if (resource == null) {
            throw new ResourceNotFoundException();
        }
        return resource;
    }
}