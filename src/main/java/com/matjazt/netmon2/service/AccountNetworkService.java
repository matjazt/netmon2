package com.matjazt.netmon2.service;

import com.matjazt.netmon2.dto.AccountDto;
import com.matjazt.netmon2.dto.AccountNetworkDto;
import com.matjazt.netmon2.dto.NetworkDto;
import com.matjazt.netmon2.dto.request.SaveAccountNetworkRequest;
import com.matjazt.netmon2.entity.AccountEntity;
import com.matjazt.netmon2.entity.AccountNetworkEntity;
import com.matjazt.netmon2.entity.NetworkEntity;
import com.matjazt.netmon2.mapper.AccountMapper;
import com.matjazt.netmon2.mapper.AccountNetworkMapper;
import com.matjazt.netmon2.mapper.NetworkMapper;
import com.matjazt.netmon2.repository.AccountNetworkRepository;
import com.matjazt.netmon2.repository.AccountRepository;
import com.matjazt.netmon2.repository.NetworkRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing AccountNetwork relationships.
 *
 * <p>Manages which accounts have access to which networks.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountNetworkService {

    private final AccountNetworkRepository accountNetworkRepository;
    private final AccountRepository accountRepository;
    private final NetworkRepository networkRepository;
    private final AccountNetworkMapper accountNetworkMapper;
    private final AccountMapper accountMapper;
    private final NetworkMapper networkMapper;

    // ========== BASIC CRUD OPERATIONS ==========

    /** Find account-network relationship by ID */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AccountNetworkEntity> findById(Long id) {
        logger.trace(
                "findById: user={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        return accountNetworkRepository.findById(id);
    }

    /** Get all account-network relationships */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountNetworkEntity> findAll() {
        logger.trace(
                "findAll: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        return accountNetworkRepository.findAll();
    }

    /** Save a new account-network relationship or update existing one */
    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public AccountNetworkEntity save(AccountNetworkEntity accountNetwork) {
        logger.trace(
                "save: user={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountNetwork.getAccount() != null ? accountNetwork.getAccount().getId() : null,
                accountNetwork.getNetwork() != null ? accountNetwork.getNetwork().getId() : null);
        return accountNetworkRepository.save(accountNetwork);
    }

    /** Delete an account-network relationship */
    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public void delete(Long id) {
        logger.trace(
                "delete: user={}, id={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                id);
        accountNetworkRepository.deleteById(id);
    }

    // ========== CUSTOM QUERY METHODS ==========

    /** Find all networks accessible by an account */
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #accountId)")
    public List<AccountNetworkEntity> findByAccountId(Long accountId) {
        logger.trace(
                "findByAccountId: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId);
        var entities = accountNetworkRepository.findByAccount_Id(accountId);
        logger.trace("findByAccountId: returning {} relationships", entities.size());
        return entities;
    }

    /** Find all accounts with access to a network */
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public List<AccountNetworkEntity> findByNetworkId(Long networkId) {
        logger.trace(
                "findByNetworkId: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        var entities = accountNetworkRepository.findByNetwork_Id(networkId);
        logger.trace("findByNetworkId: returning {} relationships", entities.size());
        return entities;
    }

    /** Check if an account has access to a network */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public boolean hasAccess(Long accountId, Long networkId) {
        logger.trace(
                "hasAccess: user={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId,
                networkId);
        return accountNetworkRepository.existsByAccount_IdAndNetwork_Id(accountId, networkId);
    }

    /** Get all networks accessible by an account (direct network entities) */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkEntity> getNetworksByAccountId(Long accountId) {
        logger.trace(
                "getNetworksByAccountId: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId);
        var networks = accountNetworkRepository.findNetworksByAccountId(accountId);
        logger.trace("getNetworksByAccountId: returning {} networks", networks.size());
        return networks;
    }

    /** Get all accounts with access to a network (direct account entities) */
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public List<AccountEntity> getAccountsByNetworkId(Long networkId) {
        logger.trace(
                "getAccountsByNetworkId: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        var accounts = accountNetworkRepository.findAccountsByNetworkId(networkId);
        logger.trace("getAccountsByNetworkId: returning {} accounts", accounts.size());
        return accounts;
    }

    /** Delete the relationship between an account and a network */
    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public void revokeAccess(Long accountId, Long networkId) {
        logger.trace(
                "revokeAccess: user={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId,
                networkId);
        accountNetworkRepository.deleteByAccount_IdAndNetwork_Id(accountId, networkId);
        logger.trace(
                "revokeAccess: revoked access for accountId={}, networkId={}",
                accountId,
                networkId);
    }

    /** Grant access to a network for an account */
    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public AccountNetworkEntity grantAccess(Long accountId, Long networkId) {
        logger.trace(
                "grantAccess: user={}, accountId={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId,
                networkId);

        // Check if relationship already exists
        if (accountNetworkRepository.existsByAccount_IdAndNetwork_Id(accountId, networkId)) {
            logger.trace("grantAccess: relationship already exists");
            throw new RuntimeException(
                    "Account " + accountId + " already has access to network " + networkId);
        }

        AccountEntity account =
                accountRepository
                        .findById(accountId)
                        .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));

        NetworkEntity network =
                networkRepository
                        .findById(networkId)
                        .orElseThrow(() -> new RuntimeException("Network not found: " + networkId));

        AccountNetworkEntity relationship = new AccountNetworkEntity(account, network);
        var saved = accountNetworkRepository.save(relationship);
        logger.trace("grantAccess: granted access with id={}", saved.getId());
        return saved;
    }

    // ========== DTO SUMMARY METHODS ==========

    /** Get all account-network relationships as DTOs */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountNetworkDto> findAllSummaries() {
        logger.trace(
                "findAllSummaries: user={}",
                SecurityContextHolder.getContext().getAuthentication().getName());
        List<AccountNetworkEntity> entities = accountNetworkRepository.findAll();
        var dtos = accountNetworkMapper.toDtos(entities);
        logger.trace("findAllSummaries: returning {} relationships", dtos.size());
        return dtos;
    }

    /** Get account-network relationships by account as DTOs */
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<AccountNetworkDto> getByAccountId(Long accountId) {
        logger.trace(
                "getByAccountId: user={}, accountId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                accountId);
        var entities = accountNetworkRepository.findByAccount_Id(accountId);
        var dtos = accountNetworkMapper.toDtos(entities);
        logger.trace("getByAccountId: returning {} relationships", dtos.size());
        return dtos;
    }

    /** Get account-network relationships by network as DTOs */
    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public List<AccountNetworkDto> getByNetworkId(Long networkId) {
        logger.trace(
                "getByNetworkId: user={}, networkId={}",
                SecurityContextHolder.getContext().getAuthentication().getName(),
                networkId);
        var entities = accountNetworkRepository.findByNetwork_Id(networkId);
        var dtos = accountNetworkMapper.toDtos(entities);
        logger.trace("getByNetworkId: returning {} relationships", dtos.size());
        return dtos;
    }

    // ========== DTO SINGLE-RECORD METHODS ==========

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public Optional<AccountNetworkDto> findDtoById(Long id) {
        return findById(id).map(accountNetworkMapper::toDto);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public AccountNetworkDto saveAndReturnDto(SaveAccountNetworkRequest request, Long id) {
        AccountNetworkEntity entity = accountNetworkMapper.toEntity(request);
        if (id != null) entity.setId(id);
        return accountNetworkMapper.toDto(save(entity));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('admin', 'system')")
    public AccountNetworkDto grantAccessAndReturnDto(Long accountId, Long networkId) {
        return accountNetworkMapper.toDto(grantAccess(accountId, networkId));
    }

    @PreAuthorize("hasAnyRole('admin', 'system')")
    public List<NetworkDto> getNetworkDtosByAccountId(Long accountId) {
        return networkMapper.toDtos(getNetworksByAccountId(accountId));
    }

    @PreAuthorize(
            "hasAnyRole('admin', 'system') or"
                    + " @networkAuthorizationService.canAccess(authentication, #networkId)")
    public List<AccountDto> getAccountDtosByNetworkId(Long networkId) {
        return accountMapper.toDtos(getAccountsByNetworkId(networkId));
    }
}
