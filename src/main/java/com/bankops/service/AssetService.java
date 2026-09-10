package com.bankops.service;

import com.bankops.dto.Requests;
import com.bankops.model.Asset;
import com.bankops.repository.AssetRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AssetService {
    private final AssetRepository repository;

    public AssetService(AssetRepository repository) { this.repository = repository; }

    @Transactional(readOnly = true)
    public List<Asset> findAll() { return repository.findAllByOrderByUpdatedAtDesc(); }

    @Transactional
    public Asset create(Requests.AssetRequest request) {
        if (repository.existsByAssetCodeIgnoreCase(request.assetCode().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "资产编号已存在");
        }
        Asset asset = new Asset();
        apply(asset, request);
        return repository.save(asset);
    }

    @Transactional
    public Asset update(Long id, Requests.AssetRequest request) {
        Asset asset = repository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "资产不存在"));
        if (!asset.getAssetCode().equalsIgnoreCase(request.assetCode().trim())
                && repository.existsByAssetCodeIgnoreCase(request.assetCode().trim())) {
            throw new BusinessException(HttpStatus.CONFLICT, "资产编号已存在");
        }
        apply(asset, request);
        return repository.save(asset);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new BusinessException(HttpStatus.NOT_FOUND, "资产不存在");
        repository.deleteById(id);
    }

    private void apply(Asset asset, Requests.AssetRequest request) {
        validateIp(request.ipAddress());
        asset.setAssetCode(request.assetCode().trim().toUpperCase());
        asset.setAssetName(request.assetName().trim());
        asset.setAssetType(request.assetType().trim());
        asset.setIpAddress(request.ipAddress().trim());
        asset.setEnvironment(request.environment().trim());
        asset.setStatus(request.status());
        asset.setOwner(request.owner().trim());
        asset.setDescription(request.description() == null ? "" : request.description().trim());
    }

    private void validateIp(String ip) {
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) throw new BusinessException(HttpStatus.BAD_REQUEST, "IP地址格式不正确");
        try {
            for (String part : parts) {
                if (part.isBlank() || Integer.parseInt(part) < 0 || Integer.parseInt(part) > 255) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException ex) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "IP地址格式不正确");
        }
    }
}
