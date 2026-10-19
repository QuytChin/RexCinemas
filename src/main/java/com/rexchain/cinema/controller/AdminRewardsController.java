package com.rexchain.cinema.controller;

import com.rexchain.cinema.entity.*;
import com.rexchain.cinema.repository.*;
import com.rexchain.cinema.service.LoyaltyService;
import com.rexchain.cinema.service.MembershipService;
import com.rexchain.cinema.service.RewardRedemptionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/rewards")
public class AdminRewardsController {
    private final UserRepository users; private final MembershipService membership; private final LoyaltyService loyalty; private final RewardRepository rewards; private final RewardRedemptionService redemptions; private final RewardRedemptionRepository redemptionRepository; private final LoyaltyLedgerRepository ledger; private final LoyaltyPolicyAuditRepository policyAudits;
    public AdminRewardsController(UserRepository users,MembershipService membership,LoyaltyService loyalty,RewardRepository rewards,RewardRedemptionService redemptions,RewardRedemptionRepository redemptionRepository,LoyaltyLedgerRepository ledger,LoyaltyPolicyAuditRepository policyAudits){this.users=users;this.membership=membership;this.loyalty=loyalty;this.rewards=rewards;this.redemptions=redemptions;this.redemptionRepository=redemptionRepository;this.ledger=ledger;this.policyAudits=policyAudits;}
    @GetMapping
    String dashboard(Model model){
        List<User> members=users.findAll(); model.addAttribute("memberCount",members.size()); model.addAttribute("availablePoints",members.stream().mapToInt(User::getLoyaltyPoints).sum()); model.addAttribute("lifetimePoints",members.stream().mapToInt(User::getLifetimePoints).sum()); model.addAttribute("redeemedPoints",members.stream().mapToInt(User::getRedeemedPoints).sum());
        model.addAttribute("tierPolicies",membership.tierPolicies()); model.addAttribute("entries",ledger.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"createdAt")).stream().limit(100).toList()); model.addAttribute("policyAudits",policyAudits.findTop50ByOrderByCreatedAtDesc()); model.addAttribute("settings",membership.settings()); model.addAttribute("rewards",rewards.findAll()); model.addAttribute("redemptionCount",redemptionRepository.count()); model.addAttribute("users",members); return "admin/rewards";
    }
    @PostMapping("/settings")
    String saveSettings(@RequestParam BigDecimal amountPerPoint,@RequestParam int minimumPositivePoints,@RequestParam(defaultValue="false") boolean voucherStackingAllowed,@RequestParam(defaultValue="false") boolean redemptionEnabled,Authentication auth,RedirectAttributes redirect){try{membership.saveSettings(amountPerPoint,minimumPositivePoints,voucherStackingAllowed,redemptionEnabled,auth.getName());redirect.addFlashAttribute("success","Đã lưu cấu hình Rex Rewards");}catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/rewards";}
    @PostMapping("/tiers/{tier}")
    String saveTier(@PathVariable MembershipTier tier,@RequestParam int qualifyingThreshold,@RequestParam BigDecimal ticketDiscountPercent,@RequestParam BigDecimal pointMultiplier,@RequestParam(defaultValue="false") boolean active,Authentication auth,RedirectAttributes redirect){try{membership.saveTierPolicy(tier,qualifyingThreshold,ticketDiscountPercent,pointMultiplier,active,auth.getName());redirect.addFlashAttribute("success","Đã lưu chính sách hạng "+tier);}catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/rewards";}
    @PostMapping("/rewards/save")
    String saveReward(@RequestParam(required=false) Long id,@RequestParam String name,@RequestParam(required=false) String description,@RequestParam int pointsCost,@RequestParam BigDecimal voucherAmount,@RequestParam(defaultValue="0") BigDecimal voucherMinOrderAmount,@RequestParam int validityDays,@RequestParam(required=false) Integer redemptionLimit,@RequestParam(defaultValue="false") boolean active,RedirectAttributes redirect){try{redemptions.saveReward(id,name,description,pointsCost,voucherAmount,voucherMinOrderAmount,validityDays,redemptionLimit,active);redirect.addFlashAttribute("success","Đã lưu ưu đãi");}catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/rewards";}
    @PostMapping("/rewards/{id}/toggle")
    String toggleReward(@PathVariable Long id,RedirectAttributes redirect){Reward reward=rewards.findById(id).orElseThrow();reward.setActive(!reward.isActive());rewards.save(reward);redirect.addFlashAttribute("success","Đã cập nhật trạng thái ưu đãi");return "redirect:/admin/rewards";}
    @PostMapping("/adjust")
    String adjust(@RequestParam Long userId,@RequestParam int points,@RequestParam String reason,Authentication auth,RedirectAttributes redirect){try{loyalty.adjustAvailablePoints(userId,points,reason,auth.getName());redirect.addFlashAttribute("success","Đã ghi nhận điều chỉnh điểm và ledger");}catch(Exception ex){redirect.addFlashAttribute("error",ex.getMessage());}return "redirect:/admin/rewards";}
}
