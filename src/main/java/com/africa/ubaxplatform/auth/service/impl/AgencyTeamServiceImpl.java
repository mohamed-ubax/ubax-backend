package com.africa.ubaxplatform.auth.service.impl;

import com.africa.ubaxplatform.auth.dto.AgencyTeamMemberResponse;
import com.africa.ubaxplatform.auth.dto.InviteTeamMemberRequest;
import com.africa.ubaxplatform.auth.dto.UpdateTeamMemberRoleRequest;
import com.africa.ubaxplatform.auth.entity.Agency;
import com.africa.ubaxplatform.auth.entity.User;
import com.africa.ubaxplatform.auth.mapper.AgencyTeamMapper;
import com.africa.ubaxplatform.auth.repository.UserRepository;
import com.africa.ubaxplatform.auth.service.interfaces.AgencyTeamService;
import com.africa.ubaxplatform.common.constants.ResponseMessageConstants;
import com.africa.ubaxplatform.common.exception.BadRequestException;
import com.africa.ubaxplatform.common.exception.ConflictException;
import com.africa.ubaxplatform.common.exception.CustomException;
import com.africa.ubaxplatform.common.exception.NotFoundException;
import com.africa.ubaxplatform.common.exception.UnAuthorizedException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** {@inheritDoc} */
@Service
@RequiredArgsConstructor
public class AgencyTeamServiceImpl implements AgencyTeamService {

  private final UserRepository userRepository;

  /** {@inheritDoc} */
  @Override
  @Transactional(readOnly = true)
  public List<AgencyTeamMemberResponse> listTeam(String callerKeycloakId) throws CustomException {
    User caller = findCallerWithAgency(callerKeycloakId);
    return userRepository.findActiveByAgencyId(caller.getAgency().getId()).stream()
        .map(AgencyTeamMapper::toResponse)
        .toList();
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public AgencyTeamMemberResponse inviteMember(
      String callerKeycloakId, InviteTeamMemberRequest request) throws CustomException {

    User caller = findCallerWithAgency(callerKeycloakId);
    Agency agency = caller.getAgency();

    User target =
        userRepository
            .findByEmail(request.email())
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException(
                            "Aucun compte UBAX trouvé pour l'email : " + request.email()),
                        ResponseMessageConstants.AGENCY_TEAM_MEMBER_NOT_FOUND));

    if (target.hasAgency()) {
      throw new CustomException(
          new ConflictException(
              "L'utilisateur est déjà rattaché à une agence (id="
                  + target.getAgency().getId()
                  + ")"),
          ResponseMessageConstants.AGENCY_TEAM_MEMBER_ALREADY_IN_AGENCY);
    }

    target.setAgency(agency);
    target.setPartnerRole(request.partnerRole());
    userRepository.save(target);

    return AgencyTeamMapper.toResponse(target);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public AgencyTeamMemberResponse updateMemberRole(
      String callerKeycloakId, UUID memberId, UpdateTeamMemberRoleRequest request)
      throws CustomException {

    User caller = findCallerWithAgency(callerKeycloakId);
    User member = findMemberInSameAgency(caller, memberId);

    member.setPartnerRole(request.partnerRole());
    userRepository.save(member);

    return AgencyTeamMapper.toResponse(member);
  }

  /** {@inheritDoc} */
  @Override
  @Transactional
  public void removeMember(String callerKeycloakId, UUID memberId) throws CustomException {
    User caller = findCallerWithAgency(callerKeycloakId);

    if (caller.getId().equals(memberId)) {
      throw new CustomException(
          new BadRequestException("Un directeur ne peut pas se retirer lui-même de l'équipe"),
          ResponseMessageConstants.AGENCY_TEAM_CANNOT_REMOVE_SELF);
    }

    User member = findMemberInSameAgency(caller, memberId);

    member.setAgency(null);
    member.setPartnerRole(null);
    userRepository.save(member);
  }

  // ── Helpers ────────────────────────────────────────────────────

  private User findCallerWithAgency(String keycloakId) throws CustomException {
    User caller =
        userRepository
            .findByKeycloakId(keycloakId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Utilisateur introuvable : " + keycloakId),
                        ResponseMessageConstants.USER_NOT_FOUND));

    if (!caller.hasAgency()) {
      throw new CustomException(
          new UnAuthorizedException("L'appelant n'est pas rattaché à une agence"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }
    return caller;
  }

  private User findMemberInSameAgency(User caller, UUID memberId) throws CustomException {
    User member =
        userRepository
            .findById(memberId)
            .orElseThrow(
                () ->
                    new CustomException(
                        new NotFoundException("Membre introuvable : id=" + memberId),
                        ResponseMessageConstants.AGENCY_TEAM_MEMBER_NOT_FOUND));

    if (!member.hasAgency() || !member.getAgency().getId().equals(caller.getAgency().getId())) {
      throw new CustomException(
          new UnAuthorizedException(
              "Le membre cible n'appartient pas à la même agence que l'appelant"),
          ResponseMessageConstants.USER_FORBIDDEN);
    }
    return member;
  }
}
